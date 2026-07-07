using System.Data;
using System.Security.Claims;
using Dapper;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Data.SqlClient;

namespace DindoriPranitAPI.Controllers
{
    [ApiController]
    [Route("api/v1/[controller]")]
    [Authorize]
    public class NotificationsController : ControllerBase
    {
        private readonly IConfiguration _configuration;
        private readonly string _connectionString;

        public NotificationsController(IConfiguration configuration)
        {
            _configuration = configuration;
            _connectionString = _configuration.GetConnectionString("DefaultConnection") 
                ?? throw new InvalidOperationException("DefaultConnection connection string not found.");
        }

        [HttpGet]
        public async Task<IActionResult> GetNotifications()
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId)) return Unauthorized();

            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            try
            {
                var list = await connection.QueryAsync<dynamic>(
                    "sp_UserNotification_GetByUserId",
                    new { UserId = userId },
                    commandType: CommandType.StoredProcedure
                );

                return Ok(new { success = true, data = list });
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"Error fetching notifications for {userId}: {ex}");
                return StatusCode(500, new { success = false, message = "An internal server error occurred while fetching notifications." });
            }
        }

        [HttpPut("{id}/read")]
        public async Task<IActionResult> MarkAsRead(int id)
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId)) return Unauthorized();

            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            try
            {
                await connection.ExecuteAsync(
                    "sp_UserNotification_MarkRead",
                    new { Id = id, UserId = userId },
                    commandType: CommandType.StoredProcedure
                );

                return Ok(new { success = true, message = "Notification marked as read." });
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"Error marking notification {id} as read: {ex}");
                return StatusCode(500, new { success = false, message = "An internal server error occurred while marking notification as read." });
            }
        }
    }
}
