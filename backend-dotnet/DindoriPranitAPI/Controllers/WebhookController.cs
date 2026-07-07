using System;
using System.Data;
using System.Security.Claims;
using System.Threading.Tasks;
using Dapper;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Data.SqlClient;

namespace DindoriPranitAPI.Controllers
{
    [ApiController]
    [Route("api/v1/[controller]")]
    [Authorize(Roles = "Admin")] // Restricted to Admin role
    public class WebhookController : ControllerBase
    {
        private readonly IConfiguration _configuration;
        private readonly string _connectionString;
        private readonly Services.WebhookService _webhookService;

        public WebhookController(IConfiguration configuration, Services.WebhookService webhookService)
        {
            _configuration = configuration;
            _webhookService = webhookService;
            _connectionString = _configuration.GetConnectionString("DefaultConnection") 
                ?? throw new InvalidOperationException("DefaultConnection connection string not found.");
        }

        [HttpGet]
        public async Task<IActionResult> GetSettings()
        {
            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            var settings = await connection.QueryAsync<dynamic>(
                "SELECT Id AS id, Url AS url, Secret AS secret, IsActive AS isActive FROM WebhookSettings"
            );

            return Ok(new { success = true, data = settings });
        }

        [HttpPost]
        public async Task<IActionResult> SaveSettings([FromBody] SaveWebhookRequest request)
        {
            if (string.IsNullOrWhiteSpace(request.Url) || !Uri.IsWellFormedUriString(request.Url, UriKind.Absolute))
            {
                return BadRequest(new { success = false, message = "A valid absolute destination URL is required." });
            }

            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            try
            {
                await connection.ExecuteAsync(
                    "sp_WebhookSettings_Save",
                    new
                    {
                        Url = request.Url,
                        Secret = request.Secret,
                        IsActive = request.IsActive
                    },
                    commandType: CommandType.StoredProcedure
                );

                return Ok(new { success = true, message = "Webhook settings saved successfully." });
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"Error saving webhook: {ex.Message}");
                return StatusCode(500, new { success = false, message = "An internal database error occurred." });
            }
        }

        [HttpPost("test")]
        public async Task<IActionResult> TestWebhook()
        {
            try
            {
                // Send dummy accounting payload to test connection
                var mockPayload = new
                {
                    ledgerId = 9999,
                    bookingId = "test_booking_12345",
                    amount = 1500.00,
                    gurujiShare = 450.00,
                    trustShare = 1050.00,
                    type = "PaymentSync",
                    status = "Success",
                    syncedAt = DateTime.UtcNow
                };

                await _webhookService.TriggerWebhookAsync("tally.sync.test", mockPayload);

                return Ok(new { success = true, message = "Test webhook trigger payload dispatched." });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { success = false, message = $"Test trigger failed: {ex.Message}" });
            }
        }
    }

    public class SaveWebhookRequest
    {
        public string Url { get; set; } = string.Empty;
        public string? Secret { get; set; }
        public bool IsActive { get; set; } = true;
    }
}
