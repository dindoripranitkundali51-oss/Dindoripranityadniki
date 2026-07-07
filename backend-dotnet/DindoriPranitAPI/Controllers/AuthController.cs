// AuthController - Handles user registration, credentials generation, password, and OTP login - V6
using System.Data;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using Dapper;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Data.SqlClient;
using Microsoft.IdentityModel.Tokens;
using Microsoft.AspNetCore.RateLimiting;
using System.ComponentModel.DataAnnotations;

using Microsoft.AspNetCore.Authorization;

namespace DindoriPranitAPI.Controllers
{
    [ApiController]
    [Route("api/v1/[controller]")]
    public class AuthController : ControllerBase
    {
        private readonly IConfiguration _configuration;
        private readonly string _connectionString;
        private readonly DindoriPranitAPI.Services.EmailService _emailService;
        private readonly Services.FcmService _fcmService;

        public AuthController(
            IConfiguration configuration,
            DindoriPranitAPI.Services.EmailService emailService,
            Services.FcmService fcmService)
        {
            _configuration = configuration;
            _connectionString = _configuration.GetConnectionString("DefaultConnection") 
                ?? throw new InvalidOperationException("DefaultConnection connection string not found.");
            _emailService = emailService;
            _fcmService = fcmService;
        }

        private string HashPassword(string password, string salt)
        {
            using var sha256 = System.Security.Cryptography.SHA256.Create();
            var combinedBytes = Encoding.UTF8.GetBytes(password + salt);
            var hashBytes = sha256.ComputeHash(combinedBytes);
            return Convert.ToBase64String(hashBytes);
        }

        private string GenerateRandomPassword()
        {
            const string chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
            var random = new Random();
            return new string(Enumerable.Repeat(chars, 8)
                .Select(s => s[random.Next(s.Length)]).ToArray());
        }

        [HttpPost("login")]
        [EnableRateLimiting("LoginPolicy")]
        public async Task<IActionResult> Login([FromBody] LoginRequest request)
        {
            if (string.IsNullOrWhiteSpace(request.Mobile))
            {
                return BadRequest(new { success = false, message = "Mobile number / Email is required." });
            }

            if (string.IsNullOrWhiteSpace(request.Password) && string.IsNullOrWhiteSpace(request.Otp))
            {
                return BadRequest(new { success = false, message = "Either password or OTP must be provided." });
            }

            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            // 1. Verify OTP if provided
            if (!string.IsNullOrWhiteSpace(request.Otp))
            {
                var otpRecord = await connection.QueryFirstOrDefaultAsync<dynamic>(
                    "SELECT * FROM LoginOtps WHERE Mobile = @Mobile",
                    new { Mobile = request.Mobile }
                );

                if (otpRecord == null)
                {
                    return BadRequest(new { success = false, message = "No active OTP request found for this mobile / email." });
                }

                if (DateTime.UtcNow > (DateTime)otpRecord.ExpiresAt)
                {
                    return BadRequest(new { success = false, message = "OTP has expired." });
                }

                var calculatedHash = HashPassword(request.Otp, "loginsalt");
                if (calculatedHash != otpRecord.OtpHash.ToString())
                {
                    return Unauthorized(new { success = false, message = "Invalid OTP code." });
                }

                // OTP is verified. Delete it to prevent reuse.
                await connection.ExecuteAsync(
                    "DELETE FROM LoginOtps WHERE Mobile = @Mobile",
                    new { Mobile = request.Mobile }
                );
            }

            // 2. Fetch Profile
            dynamic? profile = null;
            string role = "Yajman";

            // Check Users
            var user = await connection.QueryFirstOrDefaultAsync<dynamic>(
                "sp_User_GetByMobile",
                new { Mobile = request.Mobile },
                commandType: CommandType.StoredProcedure
            );

            if (user != null)
            {
                if (user.Status == "Blocked" || user.Status == "Deleted")
                {
                    return StatusCode(403, new { success = false, message = "Your account is blocked or deleted." });
                }
                profile = user;
                role = "Yajman";
            }
            else
            {
                // Check Guruji
                var guruji = await connection.QueryFirstOrDefaultAsync<dynamic>(
                    "sp_Guruji_GetByMobile",
                    new { Mobile = request.Mobile },
                    commandType: CommandType.StoredProcedure
                );

                if (guruji != null)
                {
                    if (guruji.Status == "Blocked" || guruji.Status == "Rejected")
                    {
                        return StatusCode(403, new { success = false, message = "Your expert profile is suspended or rejected." });
                    }
                    profile = guruji;
                    role = guruji.ExpertType.ToString();
                }
                else
                {
                    // Check Admins
                    var admin = await connection.QueryFirstOrDefaultAsync<dynamic>(
                        "SELECT * FROM Admins WHERE Email = @Email",
                        new { Email = request.Mobile }
                    );

                    if (admin != null)
                    {
                        if (admin.Status == "Blocked")
                        {
                            return StatusCode(403, new { success = false, message = "Admin account is blocked." });
                        }
                        profile = admin;
                        role = "Admin";
                    }
                }
            }

            if (profile == null)
            {
                return NotFound(new { success = false, message = "Mobile number / Email not registered." });
            }

            // 3. Verify Password if OTP was NOT used
            if (string.IsNullOrWhiteSpace(request.Otp))
            {
                string dbHash = profile.PasswordHash?.ToString() ?? string.Empty;
                if (!string.IsNullOrEmpty(dbHash))
                {
                    var inputHash = HashPassword(request.Password!, profile.Uid.ToString());
                    if (inputHash != dbHash)
                    {
                        return Unauthorized(new { success = false, message = "Invalid mobile number or password." });
                    }
                }
            }

            var token = GenerateJwtToken(profile.Uid.ToString(), role);
            return Ok(new { success = true, token, role = role, profile = profile });
        }

        [HttpPost("otp/send")]
        [EnableRateLimiting("LoginPolicy")]
        public async Task<IActionResult> SendOtp([FromBody] SendOtpRequest request)
        {
            if (string.IsNullOrWhiteSpace(request.Mobile))
            {
                return BadRequest(new { success = false, message = "Mobile number / Email is required." });
            }

            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            string? email = null;
            string? fcmToken = null;
            bool userExists = false;

            // Try to find user in Users
            var user = await connection.QueryFirstOrDefaultAsync<dynamic>(
                "sp_User_GetByMobile",
                new { Mobile = request.Mobile },
                commandType: CommandType.StoredProcedure
            );

            if (user != null)
            {
                email = user.Email?.ToString();
                fcmToken = user.FcmToken?.ToString();
                userExists = true;
            }
            else
            {
                // Try to find user in Guruji
                var guruji = await connection.QueryFirstOrDefaultAsync<dynamic>(
                    "sp_Guruji_GetByMobile",
                    new { Mobile = request.Mobile },
                    commandType: CommandType.StoredProcedure
                );

                if (guruji != null)
                {
                    email = guruji.Email?.ToString();
                    fcmToken = guruji.FcmToken?.ToString();
                    userExists = true;
                }
                else
                {
                    // Try to find in Admins
                    var admin = await connection.QueryFirstOrDefaultAsync<dynamic>(
                        "SELECT * FROM Admins WHERE Email = @Email",
                        new { Email = request.Mobile }
                    );

                    if (admin != null)
                    {
                        email = admin.Email?.ToString();
                        userExists = true;
                    }
                }
            }

            if (!userExists)
            {
                return NotFound(new { success = false, message = "Mobile number / Email is not registered." });
            }

            // Generate a 6-digit random OTP
            var otp = new Random().Next(100000, 999999).ToString();
            var otpHash = HashPassword(otp, "loginsalt");
            var expiresAt = DateTime.UtcNow.AddMinutes(10);

            // Save OTP in Database
            await connection.ExecuteAsync(
                @"MERGE INTO LoginOtps AS target
                  USING (SELECT @Mobile AS Mobile) AS source
                  ON (target.Mobile = source.Mobile)
                  WHEN MATCHED THEN
                      UPDATE SET OtpHash = @OtpHash, ExpiresAt = @ExpiresAt
                  WHEN NOT MATCHED THEN
                      INSERT (Mobile, OtpHash, ExpiresAt) VALUES (@Mobile, @OtpHash, @ExpiresAt);",
                new { Mobile = request.Mobile, OtpHash = otpHash, ExpiresAt = expiresAt }
            );

            // Send via Email (free)
            if (!string.IsNullOrWhiteSpace(email))
            {
                _ = _emailService.SendEmailAsync(
                    email,
                    "Dindori Pranit Yadnyiki - Login OTP",
                    $"Your One-Time Password (OTP) for login is: {otp}\n\nThis OTP is valid for 10 minutes. Please do not share it with anyone."
                );
            }

            // Send via FCM notification (free)
            if (!string.IsNullOrEmpty(fcmToken))
            {
                _ = _fcmService.SendNotificationAsync(
                    fcmToken,
                    "Login OTP Code",
                    $"Your One-Time Password (OTP) for login is: {otp}"
                );
            }

            return Ok(new { 
                success = true, 
                message = "OTP sent successfully."
            });
        }

        [HttpPost("register/user")]
        [EnableRateLimiting("LoginPolicy")]
        public async Task<IActionResult> RegisterUser([FromBody] UserRegisterRequest request)
        {
            try
            {
                using var connection = new SqlConnection(_connectionString);
                await connection.OpenAsync();

                var uid = Guid.NewGuid().ToString();
                var rawPassword = GenerateRandomPassword();
                var passwordHash = HashPassword(rawPassword, uid);

                var parameters = new
                {
                    Uid = uid,
                    request.FullName,
                    request.Mobile,
                    request.Email,
                    request.Address,
                    request.District,
                    request.Pincode,
                    Lat = request.Lat ?? 0.0,
                    Lng = request.Lng ?? 0.0,
                    PasswordHash = passwordHash
                };

                await connection.ExecuteAsync(
                    "sp_User_Insert",
                    parameters,
                    commandType: CommandType.StoredProcedure
                );

                if (!string.IsNullOrWhiteSpace(request.Email))
                {
                    _ = _emailService.SendEmailAsync(
                        request.Email, 
                        "Dindori Pranit Yadnyiki - Registration Credentials", 
                        $"Welcome {request.FullName},\n\nYour account has been registered successfully.\n\nYour Login Credentials:\nUsername/Mobile: {request.Mobile}\nPassword: {rawPassword}\n\nPlease keep these details secure."
                    );
                }

                var token = GenerateJwtToken(uid, "Yajman");
                return Ok(new { success = true, token, role = "Yajman", uid = uid, password = rawPassword });
            }
            catch (SqlException ex) when (ex.Number == 2627) // Unique constraint violation
            {
                return Conflict(new { success = false, message = "Mobile number already registered." });
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"Error registering user: {ex}");
                return StatusCode(500, new { success = false, message = "An internal server error occurred during registration." });
            }
        }

        [HttpPost("register/expert")]
        [EnableRateLimiting("LoginPolicy")]
        public async Task<IActionResult> RegisterExpert([FromBody] ExpertRegisterRequest request)
        {
            try
            {
                using var connection = new SqlConnection(_connectionString);
                await connection.OpenAsync();

                var uid = Guid.NewGuid().ToString();
                var rawPassword = GenerateRandomPassword();
                var passwordHash = HashPassword(rawPassword, uid);

                var parameters = new
                {
                    Uid = uid,
                    request.FullName,
                    request.Mobile,
                    request.Email,
                    request.Address,
                    request.District,
                    request.Pincode,
                    Lat = request.Lat ?? 0.0,
                    Lng = request.Lng ?? 0.0,
                    request.Expertises,
                    request.ExpertType, // "Guruji" or "VastuExpert"
                    PasswordHash = passwordHash
                };

                await connection.ExecuteAsync(
                    "sp_Guruji_Insert",
                    parameters,
                    commandType: CommandType.StoredProcedure
                );

                // Set initial mock URLs, PAN/Aadhar numbers, and KycStatus as 'Submitted'
                string panMockUrl = $"https://dindoripranit.storage.local/kyc/{uid}_pan.jpg";
                string aadharMockUrl = $"https://dindoripranit.storage.local/kyc/{uid}_aadhar.jpg";

                await connection.ExecuteAsync(
                    @"UPDATE Guruji 
                      SET PanCardUrl = @PanUrl, 
                          AadharCardUrl = @AadharUrl, 
                          PanNumber = @PanNumber,
                          AadharNumber = @AadharNumber,
                          HomeLat = @HomeLat,
                          HomeLng = @HomeLng,
                          KycStatus = 'Submitted', 
                          Status = 'Pending' 
                      WHERE Uid = @Uid",
                    new { 
                        Uid = uid, 
                        PanUrl = panMockUrl, 
                        AadharUrl = aadharMockUrl, 
                        PanNumber = request.PanNumber ?? "",
                        AadharNumber = request.AadharNumber ?? "",
                        HomeLat = parameters.Lat,
                        HomeLng = parameters.Lng
                    }
                );

                if (!string.IsNullOrWhiteSpace(request.Email))
                {
                    _ = _emailService.SendEmailAsync(
                        request.Email, 
                        "Dindori Pranit Yadnyiki - Expert Profile Registered", 
                        $"Welcome {request.FullName},\n\nYour profile has been submitted for verification.\n\nYour Login Credentials:\nUsername/Mobile: {request.Mobile}\nPassword: {rawPassword}\n\nNote: Your profile will be active once approved by the Admin."
                    );
                }

                // Generate JWT Auth Token for instant login capability (but profile is still Pending approval)
                var token = GenerateJwtToken(uid, request.ExpertType);

                return Ok(new { 
                    success = true, 
                    message = "Registration successful. Profile pending admin verification.", 
                    uid = uid,
                    token,
                    role = request.ExpertType,
                    password = rawPassword,
                    profile = new {
                        Uid = uid,
                        FullName = request.FullName,
                        Mobile = request.Mobile,
                        Email = request.Email,
                        Address = request.Address,
                        District = request.District,
                        Pincode = request.Pincode,
                        ExpertType = request.ExpertType,
                        Status = "Pending",
                        KycStatus = "Submitted"
                    }
                });
            }
            catch (SqlException ex) when (ex.Number == 2627)
            {
                return Conflict(new { success = false, message = "Mobile number already registered." });
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"Error registering expert: {ex}");
                return StatusCode(500, new { success = false, message = "An internal server error occurred during registration." });
            }
        }

        private string GenerateJwtToken(string uid, string role)
        {
            var tokenHandler = new JwtSecurityTokenHandler();
            var jwtSecret = _configuration["Jwt:Secret"] ?? throw new InvalidOperationException("JWT Secret key is not configured.");
            var key = Encoding.ASCII.GetBytes(jwtSecret);

            var tokenDescriptor = new SecurityTokenDescriptor
            {
                Subject = new ClaimsIdentity(new[]
                {
                    new Claim(ClaimTypes.NameIdentifier, uid),
                    new Claim(ClaimTypes.Role, role)
                }),
                Expires = DateTime.UtcNow.AddDays(Convert.ToDouble(_configuration["Jwt:ExpiryDays"] ?? "30")),
                SigningCredentials = new SigningCredentials(new SymmetricSecurityKey(key), SecurityAlgorithms.HmacSha256Signature)
            };

            var token = tokenHandler.CreateToken(tokenDescriptor);
            return tokenHandler.WriteToken(token);
        }

        [HttpPut("fcm-token")]
        [Authorize]
        public async Task<IActionResult> UpdateFcmToken([FromBody] UpdateFcmTokenRequest request)
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId)) return Unauthorized();

            if (string.IsNullOrWhiteSpace(request.FcmToken))
            {
                return BadRequest(new { success = false, message = "FCM Token is required." });
            }

            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            try
            {
                // First check if user is a Yajman (Users table)
                var userExists = await connection.ExecuteScalarAsync<int>(
                    "SELECT COUNT(*) FROM Users WHERE Uid = @Uid",
                    new { Uid = userId }
                );

                if (userExists > 0)
                {
                    await connection.ExecuteAsync(
                        "sp_User_UpdateFcmToken",
                        new { Uid = userId, FcmToken = request.FcmToken },
                        commandType: CommandType.StoredProcedure
                    );
                    return Ok(new { success = true, message = "User FCM Token updated successfully." });
                }

                // If not, check if Guruji
                var expertExists = await connection.ExecuteScalarAsync<int>(
                    "SELECT COUNT(*) FROM Guruji WHERE Uid = @Uid",
                    new { Uid = userId }
                );

                if (expertExists > 0)
                {
                    await connection.ExecuteAsync(
                        "UPDATE Guruji SET FcmToken = @FcmToken, UpdatedAt = GETUTCDATE() WHERE Uid = @Uid",
                        new { Uid = userId, FcmToken = request.FcmToken }
                    );
                    return Ok(new { success = true, message = "Expert FCM Token updated successfully." });
                }

                return NotFound(new { success = false, message = "User/Expert not found." });
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"Error updating FCM token for {userId}: {ex}");
                return StatusCode(500, new { success = false, message = "An internal server error occurred while updating FCM token." });
            }
        }
    }

    public class UpdateFcmTokenRequest
    {
        [Required]
        public string FcmToken { get; set; } = string.Empty;
    }

    public class LoginRequest
    {
        [Required]
        public string Mobile { get; set; } = string.Empty;

        public string? Password { get; set; }
        public string? Otp { get; set; }
    }

    public class SendOtpRequest
    {
        [Required]
        public string Mobile { get; set; } = string.Empty;
    }

    public class UserRegisterRequest
    {
        [Required]
        [StringLength(100, MinimumLength = 2)]
        public string FullName { get; set; } = string.Empty;

        [Required]
        [Phone]
        [StringLength(15, MinimumLength = 10)]
        public string Mobile { get; set; } = string.Empty;

        [EmailAddress]
        [StringLength(100)]
        public string? Email { get; set; }

        [StringLength(250)]
        public string? Address { get; set; }

        [StringLength(100)]
        public string? District { get; set; }

        [StringLength(10)]
        public string? Pincode { get; set; }

        public double? Lat { get; set; }
        public double? Lng { get; set; }
    }

    public class ExpertRegisterRequest
    {
        [Required]
        [StringLength(100, MinimumLength = 2)]
        public string FullName { get; set; } = string.Empty;

        [Required]
        [Phone]
        [StringLength(15, MinimumLength = 10)]
        public string Mobile { get; set; } = string.Empty;

        [EmailAddress]
        [StringLength(100)]
        public string? Email { get; set; }

        [StringLength(250)]
        public string? Address { get; set; }

        [StringLength(100)]
        public string? District { get; set; }

        [StringLength(10)]
        public string? Pincode { get; set; }

        public double? Lat { get; set; }
        public double? Lng { get; set; }

        [StringLength(500)]
        public string Expertises { get; set; } = string.Empty; // Comma-separated: 'Satyanarayan,Rudrabhishek'

        [Required]
        [StringLength(50)]
        public string ExpertType { get; set; } = "Guruji"; // "Guruji" or "VastuExpert"

        [Required]
        [StringLength(10)]
        public string PanNumber { get; set; } = string.Empty;

        [Required]
        [StringLength(12)]
        public string AadharNumber { get; set; } = string.Empty;
    }
}
