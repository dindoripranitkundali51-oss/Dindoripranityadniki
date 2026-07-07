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

        public AuthController(IConfiguration configuration)
        {
            _configuration = configuration;
            _connectionString = _configuration.GetConnectionString("DefaultConnection") 
                ?? throw new InvalidOperationException("DefaultConnection connection string not found.");
        }

        [HttpPost("login")]
        [EnableRateLimiting("LoginPolicy")]
        public async Task<IActionResult> Login([FromBody] LoginRequest request)
        {
            if (string.IsNullOrWhiteSpace(request.Mobile))
            {
                return BadRequest(new { success = false, message = "Mobile number is required." });
            }

            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            // 1. Try to find the user in Users table
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

                var token = GenerateJwtToken(user.Uid.ToString(), "Yajman");
                return Ok(new { success = true, token, role = "Yajman", profile = user });
            }

            // 2. Try to find the user in Guruji table
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

                var token = GenerateJwtToken(guruji.Uid.ToString(), guruji.ExpertType.ToString()); // "Guruji" or "VastuExpert"
                return Ok(new { success = true, token, role = guruji.ExpertType.ToString(), profile = guruji });
            }

            // 3. Try to find in Admins table
            var admin = await connection.QueryFirstOrDefaultAsync<dynamic>(
                "SELECT * FROM Admins WHERE Email = @Email",
                new { Email = request.Mobile } // Admin logs in using email address
            );

            if (admin != null)
            {
                if (admin.Status == "Blocked")
                {
                    return StatusCode(403, new { success = false, message = "Admin account is blocked." });
                }

                var token = GenerateJwtToken(admin.Uid.ToString(), "Admin");
                return Ok(new { success = true, token, role = "Admin", profile = admin });
            }

            return NotFound(new { success = false, message = "Mobile number / Email not registered." });
        }

        [HttpPost("register/user")]
        [EnableRateLimiting("LoginPolicy")]
        public async Task<IActionResult> RegisterUser([FromBody] UserRegisterRequest request)
        {
            try
            {
                using var connection = new SqlConnection(_connectionString);
                await connection.OpenAsync();

                var parameters = new
                {
                    Uid = Guid.NewGuid().ToString(),
                    request.FullName,
                    request.Mobile,
                    request.Email,
                    request.Address,
                    request.District,
                    request.Pincode,
                    Lat = request.Lat ?? 0.0,
                    Lng = request.Lng ?? 0.0
                };

                await connection.ExecuteAsync(
                    "sp_User_Insert",
                    parameters,
                    commandType: CommandType.StoredProcedure
                );

                var token = GenerateJwtToken(parameters.Uid, "Yajman");
                return Ok(new { success = true, token, role = "Yajman", uid = parameters.Uid });
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

                var parameters = new
                {
                    Uid = Guid.NewGuid().ToString(),
                    request.FullName,
                    request.Mobile,
                    request.Email,
                    request.Address,
                    request.District,
                    request.Pincode,
                    Lat = request.Lat ?? 0.0,
                    Lng = request.Lng ?? 0.0,
                    request.Expertises,
                    request.ExpertType // "Guruji" or "VastuExpert"
                };

                await connection.ExecuteAsync(
                    "sp_Guruji_Insert",
                    parameters,
                    commandType: CommandType.StoredProcedure
                );

                // Set initial mock URLs, PAN/Aadhar numbers, and KycStatus as 'Submitted'
                string panMockUrl = $"https://dindoripranit.storage.local/kyc/{parameters.Uid}_pan.jpg";
                string aadharMockUrl = $"https://dindoripranit.storage.local/kyc/{parameters.Uid}_aadhar.jpg";

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
                        Uid = parameters.Uid, 
                        PanUrl = panMockUrl, 
                        AadharUrl = aadharMockUrl, 
                        PanNumber = request.PanNumber ?? "",
                        AadharNumber = request.AadharNumber ?? "",
                        HomeLat = parameters.Lat,
                        HomeLng = parameters.Lng
                    }
                );

                // Generate JWT Auth Token for instant login capability (but profile is still Pending approval)
                var token = GenerateJwtToken(parameters.Uid, request.ExpertType);

                return Ok(new { 
                    success = true, 
                    message = "Registration successful. Profile pending admin verification.", 
                    uid = parameters.Uid,
                    token,
                    role = request.ExpertType,
                    profile = new {
                        Uid = parameters.Uid,
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
        [Phone]
        [StringLength(15, MinimumLength = 10)]
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
