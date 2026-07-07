using System;
using System.Data;
using System.Security.Claims;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using Dapper;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Data.SqlClient;

namespace DindoriPranitAPI.Controllers
{
    [ApiController]
    [Route("api/v1/[controller]")]
    [Authorize]
    public class KycController : ControllerBase
    {
        private readonly IConfiguration _configuration;
        private readonly string _connectionString;

        public KycController(IConfiguration configuration)
        {
            _configuration = configuration;
            _connectionString = _configuration.GetConnectionString("DefaultConnection") 
                ?? throw new InvalidOperationException("DefaultConnection connection string not found.");
        }

        [HttpPost("submit")]
        public async Task<IActionResult> SubmitKyc([FromBody] SubmitKycRequest request)
        {
            var expertId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(expertId)) return Unauthorized();

            if (string.IsNullOrWhiteSpace(request.PanNumber) || string.IsNullOrWhiteSpace(request.AadharNumber))
            {
                return BadRequest(new { success = false, message = "PAN and Aadhar card numbers are required." });
            }

            // Mock OCR Format Validation (Regex)
            var panRegex = new Regex(@"^[A-Z]{5}[0-9]{4}[A-Z]{1}$");
            var aadharRegex = new Regex(@"^[0-9]{12}$");

            bool isPanValid = panRegex.IsMatch(request.PanNumber.Trim().ToUpper());
            bool isAadharValid = aadharRegex.IsMatch(request.AadharNumber.Trim());

            if (!isPanValid)
            {
                return BadRequest(new { success = false, message = "OCR Validation Failed: Invalid PAN Card number format." });
            }

            if (!isAadharValid)
            {
                return BadRequest(new { success = false, message = "OCR Validation Failed: Aadhar Card must be exactly 12 numeric digits." });
            }

            // KYC Verified! Save mock doc URLs
            string panMockUrl = $"https://dindoripranit.storage.local/kyc/{expertId}_pan.jpg";
            string aadharMockUrl = $"https://dindoripranit.storage.local/kyc/{expertId}_aadhar.jpg";

            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            try
            {
                // We submit the KYC and auto-approve if valid (Mocking Instant OCR Auto-Approval!)
                string autoStatus = "Approved";

                await connection.ExecuteAsync(
                    "sp_Guruji_UpdateKyc",
                    new
                    {
                        Uid = expertId,
                        PanCardUrl = panMockUrl,
                        AadharCardUrl = aadharMockUrl,
                        KycStatus = autoStatus
                    },
                    commandType: CommandType.StoredProcedure
                );

                // Auto-activate the Guruji status to Approved so they are available for bookings
                await connection.ExecuteAsync(
                    "UPDATE Guruji SET Status = 'Approved' WHERE Uid = @Uid",
                    new { Uid = expertId }
                );

                return Ok(new
                {
                    success = true,
                    message = "Instant OCR Verification Successful! KYC approved and profile activated.",
                    kycStatus = autoStatus,
                    panUrl = panMockUrl,
                    aadharUrl = aadharMockUrl
                });
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"Error processing KYC upload: {ex.Message}");
                return StatusCode(500, new { success = false, message = "An internal error occurred during KYC processing." });
            }
        }

        [HttpGet("pending")]
        [Authorize(Roles = "Admin")] // Restricted to Admin
        public async Task<IActionResult> GetPendingKyc()
        {
            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            var list = await connection.QueryAsync<dynamic>(
                "SELECT Uid AS uid, FullName AS fullName, Mobile AS mobile, PanCardUrl AS panCardUrl, AadharCardUrl AS aadharCardUrl, KycStatus AS kycStatus, PanNumber AS panNumber, AadharNumber AS aadharNumber FROM Guruji WHERE KycStatus != 'Approved' AND PanCardUrl IS NOT NULL"
            );

            return Ok(new { success = true, data = list });
        }

        [HttpPost("verify/{uid}")]
        [Authorize(Roles = "Admin")] // Restricted to Admin
        public async Task<IActionResult> VerifyKyc(string uid, [FromBody] VerifyKycRequest request)
        {
            if (request.Status != "Approved" && request.Status != "Rejected")
            {
                return BadRequest(new { success = false, message = "Invalid status. Must be Approved or Rejected." });
            }

            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            try
            {
                if (request.Status == "Approved")
                {
                    var guruji = await connection.QuerySingleOrDefaultAsync<dynamic>(
                        "SELECT FullName, Mobile, Pincode, PanNumber, AadharNumber FROM Guruji WHERE Uid = @Uid",
                        new { Uid = uid }
                    );

                    if (guruji == null)
                    {
                        return NotFound(new { success = false, message = "Guruji profile not found." });
                    }

                    // 1. Validate PAN Card Format
                    var panRegex = new Regex(@"^[A-Z]{5}[0-9]{4}[A-Z]{1}$");
                    string pan = guruji.PanNumber ?? "";
                    if (!panRegex.IsMatch(pan.Trim().ToUpper()))
                    {
                        return BadRequest(new { success = false, message = "OCR Verification Failed: Invalid PAN Card number format." });
                    }

                    // 2. Validate Aadhar Card Format
                    var aadharRegex = new Regex(@"^[0-9]{12}$");
                    string aadhar = guruji.AadharNumber ?? "";
                    if (!aadharRegex.IsMatch(aadhar.Trim()))
                    {
                        return BadRequest(new { success = false, message = "OCR Verification Failed: Aadhar Card must be exactly 12 numeric digits." });
                    }

                    // 3. Validate FullName
                    string fullName = guruji.FullName ?? "";
                    if (string.IsNullOrWhiteSpace(fullName) || fullName.Trim().Length < 3 || Regex.IsMatch(fullName, @"[0-9]"))
                    {
                        return BadRequest(new { success = false, message = "OCR Verification Failed: Profile name mismatch or invalid characters." });
                    }

                    // 4. Validate Mobile Number
                    string mobile = guruji.Mobile ?? "";
                    if (string.IsNullOrWhiteSpace(mobile) || !Regex.IsMatch(mobile.Trim(), @"^[6-9]\d{9}$"))
                    {
                        return BadRequest(new { success = false, message = "OCR Verification Failed: Mobile number is invalid or not registered." });
                    }

                    // 5. Validate Pincode
                    string pincode = guruji.Pincode ?? "";
                    if (string.IsNullOrWhiteSpace(pincode) || !Regex.IsMatch(pincode.Trim(), @"^\d{6}$"))
                    {
                        return BadRequest(new { success = false, message = "OCR Verification Failed: Pincode must be exactly 6 numeric digits." });
                    }
                }

                await connection.ExecuteAsync(
                    "UPDATE Guruji SET KycStatus = @Status, KycVerifiedAt = @VerifiedAt, Status = @ExpertStatus WHERE Uid = @Uid",
                    new
                    {
                        Uid = uid,
                        Status = request.Status,
                        VerifiedAt = request.Status == "Approved" ? DateTime.UtcNow : (DateTime?)null,
                        ExpertStatus = request.Status == "Approved" ? "Approved" : "Pending"
                    }
                );

                return Ok(new { success = true, message = $"KYC status updated to {request.Status}." });
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"Error verifying KYC: {ex.Message}");
                return StatusCode(500, new { success = false, message = "Failed to update KYC status." });
            }
        }
    }

    public class SubmitKycRequest
    {
        public string PanNumber { get; set; } = string.Empty;
        public string AadharNumber { get; set; } = string.Empty;
    }

    public class VerifyKycRequest
    {
        public string Status { get; set; } = string.Empty;
    }
}
