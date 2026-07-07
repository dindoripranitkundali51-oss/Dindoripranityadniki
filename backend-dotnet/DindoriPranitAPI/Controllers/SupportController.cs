using System.Data;
using System.Security.Claims;
using Dapper;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Data.SqlClient;
using System.ComponentModel.DataAnnotations;

namespace DindoriPranitAPI.Controllers
{
    [ApiController]
    [Route("api/v1/[controller]")]
    [Authorize]
    public class SupportController : ControllerBase
    {
        private readonly IConfiguration _configuration;
        private readonly string _connectionString;

        public SupportController(IConfiguration configuration)
        {
            _configuration = configuration;
            _connectionString = _configuration.GetConnectionString("DefaultConnection") 
                ?? throw new InvalidOperationException("DefaultConnection connection string not found.");
        }

        [HttpPost("ticket")]
        public async Task<IActionResult> CreateTicket([FromBody] CreateTicketRequest request)
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId)) return Unauthorized();

            if (string.IsNullOrWhiteSpace(request.Subject) || string.IsNullOrWhiteSpace(request.Description))
            {
                return BadRequest(new { success = false, message = "Subject and Description are required." });
            }

            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            try
            {
                var ticketId = Guid.NewGuid().ToString();
                await connection.ExecuteAsync(
                    "sp_SupportTicket_Create",
                    new
                    {
                        Id = ticketId,
                        UserId = userId,
                        Subject = request.Subject,
                        Description = request.Description,
                        Category = request.Category ?? "General",
                        Language = request.Language ?? "mr"
                    },
                    commandType: CommandType.StoredProcedure
                );

                return Ok(new { success = true, message = "Support ticket created successfully.", ticketId });
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"Error creating support ticket for {userId}: {ex}");
                return StatusCode(500, new { success = false, message = "An internal server error occurred while creating support ticket." });
            }
        }

        [HttpGet("tickets")]
        public async Task<IActionResult> GetTickets()
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId)) return Unauthorized();

            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            try
            {
                var list = await connection.QueryAsync<dynamic>(
                    "sp_SupportTicket_GetByUserId",
                    new { UserId = userId },
                    commandType: CommandType.StoredProcedure
                );

                return Ok(new { success = true, data = list });
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"Error fetching support tickets for {userId}: {ex}");
                return StatusCode(500, new { success = false, message = "An internal server error occurred while fetching support tickets." });
            }
        }
    }

    public class CreateTicketRequest
    {
        [Required]
        [StringLength(255, MinimumLength = 3)]
        public string Subject { get; set; } = string.Empty;

        [Required]
        [MinLength(5)]
        public string Description { get; set; } = string.Empty;

        [StringLength(100)]
        public string? Category { get; set; } = "General";

        [StringLength(50)]
        public string? Language { get; set; } = "mr";
    }
}
