using System.Data;
using System.Security.Claims;
using System.Security.Cryptography;
using System.Text;
using Dapper;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Data.SqlClient;
using Microsoft.AspNetCore.RateLimiting;

namespace DindoriPranitAPI.Controllers
{
    [ApiController]
    [Route("api/v1/[controller]")]
    [Authorize] // Requires valid JWT token for all actions
    public class BookingController : ControllerBase
    {
        private readonly IConfiguration _configuration;
        private readonly string _connectionString;
        private readonly Services.FcmService _fcmService;

        public BookingController(IConfiguration configuration, Services.FcmService fcmService)
        {
            _configuration = configuration;
            _fcmService = fcmService;
            _connectionString = _configuration.GetConnectionString("DefaultConnection") 
                ?? throw new InvalidOperationException("DefaultConnection connection string not found.");
        }

        [HttpPost]
        public async Task<IActionResult> CreateBooking([FromBody] CreateBookingRequest request)
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId)) return Unauthorized();

            if (string.IsNullOrEmpty(request.ClientRequestId))
            {
                return BadRequest(new { success = false, message = "ClientRequestId is required for idempotency check." });
            }

            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            // 1. Idempotency Check (ड्युप्लिकेट बुकिंग रोखणे)
            var isDuplicate = await connection.QuerySingleAsync<bool>(
                "sp_BookingIdempotency_Check",
                new { UserId = userId, ClientRequestId = request.ClientRequestId },
                commandType: CommandType.StoredProcedure
            );

            if (isDuplicate)
            {
                // Fetch the existing booking to return it
                var existingBooking = await connection.QueryFirstOrDefaultAsync<dynamic>(
                    "SELECT TOP 1 * FROM Bookings WHERE UserId = @UserId ORDER BY CreatedAt DESC",
                    new { UserId = userId }
                );
                return Ok(new { success = true, isDuplicate = true, data = existingBooking });
            }

            // Save request key
            await connection.ExecuteAsync(
                "INSERT INTO BookingIdempotency (UserId, ClientRequestId) VALUES (@UserId, @ClientRequestId)",
                new { UserId = userId, ClientRequestId = request.ClientRequestId }
            );

            // 2. Fetch Pooja/Seva details to determine Type (Yadnyiki vs Vastu)
            var pooja = await connection.QueryFirstOrDefaultAsync<dynamic>(
                "SELECT * FROM Poojas WHERE Id = @PoojaId",
                new { PoojaId = request.PoojaId }
            );

            if (pooja == null)
            {
                return NotFound(new { success = false, message = "Pooja/Seva type not found." });
            }

            string requiredExpertType = pooja.SevaType == "Vastu" ? "VastuExpert" : "Guruji";
            decimal? amount = null;

            // 3. Create the Booking Record
            var bookingId = Guid.NewGuid().ToString();
            var booking = await connection.QueryFirstOrDefaultAsync<dynamic>(
                "sp_Booking_Create",
                new
                {
                    Id = bookingId,
                    UserId = userId,
                    PoojaId = request.PoojaId,
                    Date = request.Date,
                    Amount = amount,
                    ContactName = request.ContactName ?? "Yajman",
                    ContactPhone = request.ContactPhone ?? "",
                    Address = request.Address ?? "",
                    District = request.District ?? "",
                    Pincode = request.Pincode ?? "",
                    UserLat = request.UserLat ?? 0.0,
                    UserLng = request.UserLng ?? 0.0
                },
                commandType: CommandType.StoredProcedure
            );

            // 4. Run Matching & Auto-Assignment (गुरुजी किंवा वास्तू तज्ञ शोधून जोडणे)
            var candidates = await connection.QueryAsync<dynamic>(
                "sp_Guruji_GetAvailableForDate",
                new
                {
                    Date = request.Date,
                    RequiredExpertise = pooja.Id,
                    RequiredExpertType = requiredExpertType,
                    UserLat = request.UserLat ?? 0.0,
                    UserLng = request.UserLng ?? 0.0
                },
                commandType: CommandType.StoredProcedure
            );

            string? assignedExpertId = null;
            foreach (var candidate in candidates)
            {
                try
                {
                    // Attempt atomic lock assignment
                    await connection.ExecuteAsync(
                        "sp_Booking_AssignGuruji",
                        new { BookingId = bookingId, GurujiId = (string)candidate.Uid, Date = request.Date },
                        commandType: CommandType.StoredProcedure
                    );
                    assignedExpertId = candidate.Uid;
                    string? fcmToken = candidate.FcmToken?.ToString();
                    if (!string.IsNullOrEmpty(fcmToken))
                    {
                        _ = _fcmService.SendNotificationAsync(fcmToken, "नवीन पूजा आमंत्रण", $"तुम्हाला '{pooja.Name}' पूजेसाठी आमंत्रित केले आहे.");
                    }
                    break; // Successfully assigned!
                }
                catch (SqlException ex) when (ex.Message.Contains("GURUJI_SLOT_LOCKED"))
                {
                    // Lock failed, try next candidate
                    continue;
                }
            }

            // Reload final booking state
            var finalBooking = await connection.QueryFirstOrDefaultAsync<dynamic>(
                "sp_Booking_GetById",
                new { Id = bookingId },
                commandType: CommandType.StoredProcedure
            );

            return Ok(new { 
                success = true, 
                assigned = assignedExpertId != null, 
                assignedId = assignedExpertId,
                data = finalBooking 
            });
        }

        [HttpGet("{id}")]
        public async Task<IActionResult> GetBooking(string id)
        {
            using var connection = new SqlConnection(_connectionString);
            var booking = await connection.QueryFirstOrDefaultAsync<dynamic>(
                "sp_Booking_GetById",
                new { Id = id },
                commandType: CommandType.StoredProcedure
            );

            if (booking == null) return NotFound(new { success = false, message = "Booking not found." });
            return Ok(new { success = true, data = booking });
        }

        [HttpGet("user")]
        public async Task<IActionResult> GetUserBookings()
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId)) return Unauthorized();

            using var connection = new SqlConnection(_connectionString);
            var list = await connection.QueryAsync<dynamic>(
                "sp_Booking_GetByUserId",
                new { UserId = userId },
                commandType: CommandType.StoredProcedure
            );

            return Ok(new { success = true, data = list });
        }

        [HttpGet("expert")]
        public async Task<IActionResult> GetExpertBookings()
        {
            var expertId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(expertId)) return Unauthorized();

            using var connection = new SqlConnection(_connectionString);
            var list = await connection.QueryAsync<dynamic>(
                "sp_Booking_GetByGurujiId",
                new { GurujiId = expertId },
                commandType: CommandType.StoredProcedure
            );

            return Ok(new { success = true, data = list });
        }

        [HttpPut("{id}/status")]
        public async Task<IActionResult> UpdateStatus(string id, [FromBody] UpdateStatusRequest request)
        {
            var actorId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(actorId)) return Unauthorized();

            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            var booking = await connection.QueryFirstOrDefaultAsync<dynamic>(
                "SELECT * FROM Bookings WHERE Id = @Id",
                new { Id = id }
            );

            if (booking == null) return NotFound(new { success = false, message = "Booking not found." });

            // Lock release if cancelled or rejected
            if (request.Status == "Cancelled" || request.Status == "Rejected")
            {
                if (booking.GurujiId != null)
                {
                    await connection.ExecuteAsync(
                        "sp_BookingLock_Release",
                        new { Date = booking.Date, GurujiId = booking.GurujiId, BookingId = id },
                        commandType: CommandType.StoredProcedure
                    );
                }
            }

            await connection.ExecuteAsync(
                "sp_Booking_UpdateStatus",
                new { BookingId = id, Status = request.Status, ActorId = actorId },
                commandType: CommandType.StoredProcedure
            );

            var yajmanToken = await connection.ExecuteScalarAsync<string>(
                "SELECT FcmToken FROM Users WHERE Uid = (SELECT UserId FROM Bookings WHERE Id = @Id)",
                new { Id = id }
            );

            if (!string.IsNullOrEmpty(yajmanToken))
            {
                if (request.Status == "Accepted")
                {
                    _ = _fcmService.SendNotificationAsync(yajmanToken, "पूजा आमंत्रण स्वीकारले", "तुमच्या पूजेचे आमंत्रण गुरुजींनी स्वीकारले आहे.");
                }
                else if (request.Status == "InProgress")
                {
                    _ = _fcmService.SendNotificationAsync(yajmanToken, "पूजा सुरू झाली", "तुमची पूजा विधी आता सुरू झाली आहे.");
                }
            }

            return Ok(new { success = true, status = request.Status });
        }

        [HttpPost("{id}/otp/request")]
        [EnableRateLimiting("LoginPolicy")]
        public async Task<IActionResult> RequestOtp(string id)
        {
            var expertId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(expertId)) return Unauthorized();

            // Generate simple 4-digit OTP
            var otp = new Random().Next(1000, 9999).ToString();
            
            // Hash using SHA256 (Simple secure hashing)
            var salt = Guid.NewGuid().ToString();
            var hash = HashOtp(otp, salt);

            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();
            await connection.ExecuteAsync(
                "sp_BookingOtp_Create",
                new {
                    BookingId = id,
                    OtpHash = hash,
                    OtpSalt = salt,
                    ExpiresAt = DateTime.UtcNow.AddMinutes(15)
                },
                commandType: CommandType.StoredProcedure
            );

            var yajmanToken = await connection.ExecuteScalarAsync<string>(
                "SELECT FcmToken FROM Users WHERE Uid = (SELECT UserId FROM Bookings WHERE Id = @Id)",
                new { Id = id }
            );

            if (!string.IsNullOrEmpty(yajmanToken))
            {
                _ = _fcmService.SendNotificationAsync(yajmanToken, "पूजा समाप्तीसाठी OTP", $"तुमच्या पूजेच्या समाप्तीसाठीचा OTP कोड: {otp}");
            }

            // In production, we would send this via FCM notification to Yajman.
            // For Swagger testing, we return it in response body to help verification.
            return Ok(new { 
                success = true, 
                message = "OTP generated successfully and sent to Yajman."
            });
        }

        [HttpPost("{id}/otp/verify")]
        public async Task<IActionResult> VerifyOtp(string id, [FromBody] VerifyOtpRequest request)
        {
            var expertId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(expertId)) return Unauthorized();

            if (request.Dakshina <= 0)
            {
                return BadRequest(new { success = false, message = "Dakshina amount must be greater than zero." });
            }

            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            var otpRecord = await connection.QueryFirstOrDefaultAsync<dynamic>(
                "sp_BookingOtp_Get",
                new { BookingId = id },
                commandType: CommandType.StoredProcedure
            );

            if (otpRecord == null)
            {
                return BadRequest(new { success = false, message = "No active OTP request found for this booking." });
            }

            if (DateTime.UtcNow > (DateTime)otpRecord.ExpiresAt)
            {
                return BadRequest(new { success = false, message = "OTP has expired." });
            }

            var calculatedHash = HashOtp(request.Otp, otpRecord.OtpSalt.ToString());
            if (calculatedHash != otpRecord.OtpHash.ToString())
            {
                return BadRequest(new { success = false, message = "Invalid OTP code." });
            }

            // OTP Verified! Clean up OTP record
            await connection.ExecuteAsync(
                "sp_BookingOtp_Delete",
                new { BookingId = id },
                commandType: CommandType.StoredProcedure
            );

            // Update Dakshina amount on Booking dynamically (30% Guruji / 70% Trust split)
            await connection.ExecuteAsync(
                @"UPDATE Bookings 
                  SET Amount = @Dakshina, 
                      GurujiShare = @Dakshina * 0.3, 
                      TrustShare = @Dakshina * 0.7 
                  WHERE Id = @BookingId",
                new { BookingId = id, Dakshina = request.Dakshina }
            );

            // Mark booking as PaymentPending
            await connection.ExecuteAsync(
                "sp_Booking_UpdateStatus",
                new { BookingId = id, Status = "PaymentPending", ActorId = expertId },
                commandType: CommandType.StoredProcedure
            );

            return Ok(new { success = true, message = "OTP verified successfully. Seva completed, awaiting payment." });
        }

        [HttpPost("{id}/feedback")]
        public async Task<IActionResult> SubmitFeedback(string id, [FromBody] SubmitFeedbackRequest request)
        {
            var actorId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(actorId)) return Unauthorized();

            if (request.Rating < 1 || request.Rating > 5)
            {
                return BadRequest(new { success = false, message = "Rating must be between 1 and 5." });
            }

            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            var booking = await connection.QueryFirstOrDefaultAsync<dynamic>(
                "SELECT UserId, GurujiId FROM Bookings WHERE Id = @Id",
                new { Id = id }
            );

            if (booking == null)
            {
                return NotFound(new { success = false, message = "Booking not found." });
            }

            if (booking.UserId != actorId)
            {
                return Forbid();
            }

            string sentiment = AnalyzeSentiment(request.Rating, request.Comment);

            try
            {
                await connection.ExecuteAsync(
                    "sp_Feedback_Insert",
                    new
                    {
                        BookingId = id,
                        UserId = actorId,
                        GurujiId = (string)booking.GurujiId,
                        Rating = request.Rating,
                        Comment = request.Comment,
                        Sentiment = sentiment
                    },
                    commandType: CommandType.StoredProcedure
                );

                return Ok(new { success = true, message = "Feedback submitted successfully.", sentiment });
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"Error submitting feedback for booking {id}: {ex}");
                return StatusCode(500, new { success = false, message = "An internal server error occurred while saving feedback." });
            }
        }

        [HttpPut("location")]
        public async Task<IActionResult> UpdateLocation([FromBody] UpdateLocationRequest request)
        {
            var expertId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(expertId)) return Unauthorized();

            if (request.Lat == 0 && request.Lng == 0)
            {
                return BadRequest(new { success = false, message = "Invalid location coordinates." });
            }

            if (request.IsMock == true)
            {
                return BadRequest(new { success = false, message = "Fake GPS / Mock Location detected." });
            }

            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            try
            {
                // Retrieve Guruji's last location and UpdatedAt to calculate average speed since last update
                var lastLoc = await connection.QueryFirstOrDefaultAsync<dynamic>(
                    "SELECT Lat, Lng, UpdatedAt FROM Guruji WHERE Uid = @GurujiId",
                    new { GurujiId = expertId }
                );

                if (lastLoc != null && lastLoc.Lat != 0.0 && lastLoc.Lng != 0.0 && lastLoc.UpdatedAt != null)
                {
                    double prevLat = (double)lastLoc.Lat;
                    double prevLng = (double)lastLoc.Lng;
                    DateTime prevTime = (DateTime)lastLoc.UpdatedAt;

                    double travelDistance = CalculateDistanceInMetres(request.Lat, request.Lng, prevLat, prevLng);
                    double timeSeconds = (DateTime.UtcNow - prevTime).TotalSeconds;

                    if (timeSeconds > 2 && timeSeconds < 3600) // Within the last hour
                    {
                        double speedMps = travelDistance / timeSeconds;
                        double speedKmph = speedMps * 3.6;

                        if (speedKmph > 150.0) // Physically impossible speed limit: 150 km/h
                        {
                            // Log security threat in AuditLogs
                            await connection.ExecuteAsync(
                                @"INSERT INTO AuditLogs (AdminId, Action, TargetId, Details, ClientIp, RequestPayload, ThreatLevel, CreatedAt)
                                  VALUES (@AdminId, @Action, @TargetId, @Details, @ClientIp, @RequestPayload, @ThreatLevel, GETUTCDATE())",
                                new
                                {
                                    AdminId = "System",
                                    Action = "LOCATION_SPOOF_DETECTED",
                                    TargetId = expertId,
                                    Details = $"Spoofed location coordinates update rejected. Speed: {speedKmph:F2} km/h. Distance: {travelDistance:F2} m in {timeSeconds:F2} s.",
                                    ClientIp = HttpContext.Connection.RemoteIpAddress?.ToString() ?? "0.0.0.0",
                                    RequestPayload = $"Lat: {request.Lat}, Lng: {request.Lng}, PrevLat: {prevLat}, PrevLng: {prevLng}",
                                    ThreatLevel = "High"
                                }
                            );

                            return BadRequest(new { success = false, message = "Fake GPS spoofing detected and update rejected." });
                        }
                    }
                }

                // Update Guruji's live location coordinates
                await connection.ExecuteAsync(
                    "UPDATE Guruji SET Lat = @Lat, Lng = @Lng, UpdatedAt = GETUTCDATE() WHERE Uid = @GurujiId",
                    new { Lat = request.Lat, Lng = request.Lng, GurujiId = expertId }
                );

                // Retrieve Guruji's permanent home coordinates once
                var homeCoords = await connection.QueryFirstOrDefaultAsync<dynamic>(
                    "SELECT HomeLat, HomeLng FROM Guruji WHERE Uid = @GurujiId",
                    new { GurujiId = expertId }
                );
                double homeLat = homeCoords?.HomeLat ?? 0.0;
                double homeLng = homeCoords?.HomeLng ?? 0.0;

                // Fetch active bookings assigned to this Guruji that are currently 'Accepted', 'Departed', or 'Arrived'
                var activeBookings = await connection.QueryAsync<dynamic>(
                    "SELECT Id, DisplayId, UserLat, UserLng, UserId, Status, Date FROM Bookings WHERE GurujiId = @GurujiId AND Status IN ('Accepted', 'Departed', 'Arrived')",
                    new { GurujiId = expertId }
                );

                foreach (var booking in activeBookings)
                {
                    DateTime poojaDate = booking.Date;

                    // Cost Optimization check: Only execute distance geofencing if Pooja is scheduled for Today or Tomorrow
                    if (poojaDate > DateTime.Today.AddDays(1))
                    {
                        continue; // Skip calculations for future poojas to save map billing costs
                    }

                    double userLat = (double)booking.UserLat;
                    double userLng = (double)booking.UserLng;
                    string bookingId = booking.Id;
                    string displayId = booking.DisplayId;
                    string userId = booking.UserId;
                    string currentStatus = booking.Status;

                    double distanceToDest = CalculateDistanceInMetres(request.Lat, request.Lng, userLat, userLng);

                    // 1. Accepted -> Departed: Triggered when Guruji leaves home (> 300m away)
                    if (currentStatus == "Accepted" && homeLat != 0.0 && homeLng != 0.0)
                    {
                        double distanceFromHome = CalculateDistanceInMetres(request.Lat, request.Lng, homeLat, homeLng);
                        if (distanceFromHome > 300)
                        {
                            await connection.ExecuteAsync(
                                "UPDATE Bookings SET Status = 'Departed', UpdatedAt = GETUTCDATE() WHERE Id = @Id",
                                new { Id = bookingId }
                            );
                            await connection.ExecuteAsync(
                                "INSERT INTO BookingEvents (BookingId, Type, Status, ActorId) VALUES (@Id, 'StatusChange', 'Departed', 'System')",
                                new { Id = bookingId }
                            );

                            string? yajmanFcmToken = await connection.QueryFirstOrDefaultAsync<string>(
                                "SELECT FcmToken FROM Users WHERE Uid = @UserId",
                                new { UserId = userId }
                            );
                            if (!string.IsNullOrWhiteSpace(yajmanFcmToken))
                            {
                                await connection.ExecuteAsync(
                                    "INSERT INTO FcmQueue (FcmToken, Title, Body) VALUES (@Token, @Title, @Body)",
                                    new
                                    {
                                        Token = yajmanFcmToken,
                                        Title = "गुरुजी निघाले आहेत! / Guruji Departed!",
                                        Body = $"गुरुजी पूजेच्या ठिकाणी येण्यासाठी निघाले आहेत (Seva ID: {displayId})."
                                    }
                                );
                            }
                        }
                    }
                    // 2. Departed -> Arrived: Triggered when Guruji is within 200 meters of Pooja destination
                    else if (currentStatus == "Departed")
                    {
                        if (distanceToDest <= 200)
                        {
                            await connection.ExecuteAsync(
                                "UPDATE Bookings SET Status = 'Arrived', UpdatedAt = GETUTCDATE() WHERE Id = @Id",
                                new { Id = bookingId }
                            );
                            await connection.ExecuteAsync(
                                "INSERT INTO BookingEvents (BookingId, Type, Status, ActorId) VALUES (@Id, 'StatusChange', 'Arrived', 'System')",
                                new { Id = bookingId }
                            );

                            string? yajmanFcmToken = await connection.QueryFirstOrDefaultAsync<string>(
                                "SELECT FcmToken FROM Users WHERE Uid = @UserId",
                                new { UserId = userId }
                            );
                            string? gurujiFcmToken = await connection.QueryFirstOrDefaultAsync<string>(
                                "SELECT FcmToken FROM Guruji WHERE Uid = @GurujiId",
                                new { GurujiId = expertId }
                            );

                            if (!string.IsNullOrWhiteSpace(yajmanFcmToken))
                            {
                                await connection.ExecuteAsync(
                                    "INSERT INTO FcmQueue (FcmToken, Title, Body) VALUES (@Token, @Title, @Body)",
                                    new
                                    {
                                        Token = yajmanFcmToken,
                                        Title = "गुरुजी पोहोचले आहेत! / Guruji Arrived!",
                                        Body = $"गुरुजी पूजेच्या ठिकाणी पोहोचले आहेत (Seva ID: {displayId})."
                                    }
                                );
                            }
                            if (!string.IsNullOrWhiteSpace(gurujiFcmToken))
                            {
                                await connection.ExecuteAsync(
                                    "INSERT INTO FcmQueue (FcmToken, Title, Body) VALUES (@Token, @Title, @Body)",
                                    new
                                    {
                                        Token = gurujiFcmToken,
                                        Title = "नियोजनाच्या ठिकाणी पोहोचलात! / Reached Destination!",
                                        Body = $"तुम्ही नियोजित ठिकाणी पोहोचला आहात (Seva ID: {displayId})."
                                    }
                                );
                            }
                        }
                    }
                    // 3. Arrived -> InProgress: Triggered when Guruji enters the house (within 50 meters)
                    else if (currentStatus == "Arrived")
                    {
                        if (distanceToDest <= 50)
                        {
                            await connection.ExecuteAsync(
                                "UPDATE Bookings SET Status = 'InProgress', UpdatedAt = GETUTCDATE() WHERE Id = @Id",
                                new { Id = bookingId }
                            );
                            await connection.ExecuteAsync(
                                "INSERT INTO BookingEvents (BookingId, Type, Status, ActorId) VALUES (@Id, 'StatusChange', 'InProgress', 'System')",
                                new { Id = bookingId }
                            );

                            string? yajmanFcmToken = await connection.QueryFirstOrDefaultAsync<string>(
                                "SELECT FcmToken FROM Users WHERE Uid = @UserId",
                                new { UserId = userId }
                            );
                            string? gurujiFcmToken = await connection.QueryFirstOrDefaultAsync<string>(
                                "SELECT FcmToken FROM Guruji WHERE Uid = @GurujiId",
                                new { GurujiId = expertId }
                            );

                            if (!string.IsNullOrWhiteSpace(yajmanFcmToken))
                            {
                                await connection.ExecuteAsync(
                                    "INSERT INTO FcmQueue (FcmToken, Title, Body) VALUES (@Token, @Title, @Body)",
                                    new
                                    {
                                        Token = yajmanFcmToken,
                                        Title = "पूजा सुरू झाली! / Pooja In Progress!",
                                        Body = $"पूजा/सेवा (Seva ID: {displayId}) आता सुरू झाली आहे."
                                    }
                                );
                            }
                            if (!string.IsNullOrWhiteSpace(gurujiFcmToken))
                            {
                                await connection.ExecuteAsync(
                                    "INSERT INTO FcmQueue (FcmToken, Title, Body) VALUES (@Token, @Title, @Body)",
                                    new
                                    {
                                        Token = gurujiFcmToken,
                                        Title = "पूजा सुरू झाली! / Pooja Started!",
                                        Body = $"पूजा/सेवा (Seva ID: {displayId}) आता सुरू झाली आहे."
                                    }
                                );
                            }
                        }
                    }
                }

                return Ok(new { success = true, message = "Location updated successfully." });
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"Error updating location for expert {expertId}: {ex}");
                return StatusCode(500, new { success = false, message = "An internal server error occurred while updating location." });
            }
        }

        private double CalculateDistanceInMetres(double lat1, double lon1, double lat2, double lon2)
        {
            var R = 6371e3; // metres
            var phi1 = lat1 * Math.PI / 180;
            var phi2 = lat2 * Math.PI / 180;
            var deltaPhi = (lat2 - lat1) * Math.PI / 180;
            var deltaLambda = (lon2 - lon1) * Math.PI / 180;

            var a = Math.Sin(deltaPhi / 2) * Math.Sin(deltaPhi / 2) +
                    Math.Cos(phi1) * Math.Cos(phi2) *
                    Math.Sin(deltaLambda / 2) * Math.Sin(deltaLambda / 2);
            var c = 2 * Math.Atan2(Math.Sqrt(a), Math.Sqrt(1 - a));

            return R * c; // in metres
        }

        private string AnalyzeSentiment(int rating, string comment)
        {
            var cleanComment = comment.ToLower().Trim();
            
            var negativeWords = new[] { "bad", "poor", "late", "slow", "unsatisfied", "worst", "unhappy", "वाईट", "उशीर", "नाराज", "असमाधानी" };
            foreach (var word in negativeWords)
            {
                if (cleanComment.Contains(word)) return "Negative";
            }

            var positiveWords = new[] { "good", "nice", "great", "excellent", "happy", "blessed", "satisfied", "छान", "उत्कृष्ट", "सुंदर", "आनंद", "समाधानी" };
            foreach (var word in positiveWords)
            {
                if (cleanComment.Contains(word)) return "Positive";
            }

            if (rating >= 4) return "Positive";
            if (rating <= 2) return "Negative";
            return "Neutral";
        }

        private string HashOtp(string otp, string salt)
        {
            using var sha256 = SHA256.Create();
            var combinedBytes = Encoding.UTF8.GetBytes(otp + salt);
            var hashBytes = sha256.ComputeHash(combinedBytes);
            return Convert.ToBase64String(hashBytes);
        }

        [HttpPost("{id}/request")]
        public async Task<IActionResult> CreateServiceRequest(string id, [FromBody] CreateServiceRequest request)
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId)) return Unauthorized();

            if (string.IsNullOrWhiteSpace(request.Type))
            {
                return BadRequest(new { success = false, message = "Request Type (Reschedule/Cancel) is required." });
            }

            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            try
            {
                // Verify that the booking belongs to the logged-in user
                var booking = await connection.QueryFirstOrDefaultAsync<dynamic>(
                    "SELECT UserId FROM Bookings WHERE Id = @Id",
                    new { Id = id }
                );

                if (booking == null) return NotFound(new { success = false, message = "Booking not found." });
                if (booking.UserId != userId) return Forbid();

                DateTime? reqDate = null;
                if (request.Type == "Reschedule")
                {
                    if (string.IsNullOrWhiteSpace(request.RequestedDate))
                    {
                        return BadRequest(new { success = false, message = "RequestedDate is required for Reschedule requests." });
                    }
                    reqDate = DateTime.Parse(request.RequestedDate);
                }

                await connection.ExecuteAsync(
                    "sp_ServiceRequest_Create",
                    new
                    {
                        BookingId = id,
                        Type = request.Type,
                        RequestedDate = reqDate,
                        Reason = request.Reason
                    },
                    commandType: CommandType.StoredProcedure
                );

                return Ok(new { success = true, message = "Service request submitted successfully." });
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"Error creating service request for booking {id}: {ex}");
                return StatusCode(500, new { success = false, message = "An internal server error occurred while submitting service request." });
            }
        }

        [HttpPost("expert/availability")]
        public async Task<IActionResult> SaveAvailability([FromBody] Models.GurujiAvailabilityRequest request)
        {
            var expertId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(expertId)) return Unauthorized();

            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            try
            {
                string dateList = request.Dates != null ? string.Join(",", request.Dates) : "";

                await connection.ExecuteAsync(
                    "sp_GurujiAvailability_Save",
                    new { GurujiId = expertId, DateList = dateList },
                    commandType: CommandType.StoredProcedure
                );

                return Ok(new { success = true, message = "Availability dates saved successfully." });
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"Error saving availability for expert {expertId}: {ex}");
                return StatusCode(500, new { success = false, message = "An error occurred while saving availability." });
            }
        }

        [HttpGet("expert/availability")]
        public async Task<IActionResult> GetAvailability()
        {
            var expertId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(expertId)) return Unauthorized();

            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            try
            {
                var dates = await connection.QueryAsync<string>(
                    "sp_GurujiAvailability_Get",
                    new { GurujiId = expertId },
                    commandType: CommandType.StoredProcedure
                );

                return Ok(new { success = true, data = dates });
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"Error fetching availability for expert {expertId}: {ex}");
                return StatusCode(500, new { success = false, message = "An error occurred while fetching availability." });
            }
        }

        [HttpGet("available-dates")]
        [AllowAnonymous]
        public async Task<IActionResult> GetAvailableDates()
        {
            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            try
            {
                var dates = await connection.QueryAsync<string>(
                    "sp_Booking_GetAvailableDates",
                    commandType: CommandType.StoredProcedure
                );

                return Ok(new { success = true, data = dates });
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"Error fetching aggregate available dates: {ex}");
                return StatusCode(500, new { success = false, message = "An error occurred while fetching available dates." });
            }
        }
    }

    public class CreateBookingRequest
    {
        public string ClientRequestId { get; set; } = string.Empty;
        public string PoojaId { get; set; } = string.Empty;
        public DateTime Date { get; set; }
        public string? ContactName { get; set; }
        public string? ContactPhone { get; set; }
        public string? Address { get; set; }
        public string? District { get; set; }
        public string? Pincode { get; set; }
        public double? UserLat { get; set; }
        public double? UserLng { get; set; }
    }

    public class UpdateStatusRequest
    {
        public string Status { get; set; } = string.Empty;
    }

    public class VerifyOtpRequest
    {
        public string Otp { get; set; } = string.Empty;
        public decimal Dakshina { get; set; }
    }

    public class SubmitFeedbackRequest
    {
        public int Rating { get; set; }
        public string Comment { get; set; } = string.Empty;
    }

    public class UpdateLocationRequest
    {
        public double Lat { get; set; }
        public double Lng { get; set; }
        public bool? IsMock { get; set; }
    }

    public class CreateServiceRequest
    {
        public string Type { get; set; } = string.Empty;
        public string? RequestedDate { get; set; }
        public string? Reason { get; set; }
    }
}
