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
    [Authorize]
    public class ChatController : ControllerBase
    {
        private readonly IConfiguration _configuration;
        private readonly string _connectionString;
        private readonly Services.ChatBotService _chatBotService;

        public ChatController(IConfiguration configuration, Services.ChatBotService chatBotService)
        {
            _configuration = configuration;
            _chatBotService = chatBotService;
            _connectionString = _configuration.GetConnectionString("DefaultConnection") 
                ?? throw new InvalidOperationException("DefaultConnection connection string not found.");
        }

        [HttpPost("send")]
        public async Task<IActionResult> SendMessage([FromBody] SendMessageRequest request)
        {
            var senderId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(senderId)) return Unauthorized();

            var senderRole = User.FindFirst(ClaimTypes.Role)?.Value ?? "User";
            var senderName = senderRole == "Admin" ? "Support Admin" : "User";

            if (string.IsNullOrWhiteSpace(request.Message))
            {
                return BadRequest(new { success = false, message = "Message content cannot be empty." });
            }

            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            try
            {
                // 1. Save user's message
                await connection.ExecuteAsync(
                    "sp_ChatMessage_Insert",
                    new
                    {
                        SenderId = senderId,
                        SenderName = senderName,
                        ReceiverId = request.ReceiverId,
                        Message = request.Message
                    },
                    commandType: CommandType.StoredProcedure
                );

                // 2. Trigger FAQ Chatbot if user is messaging Admin
                if (request.ReceiverId.Equals("Admin", StringComparison.OrdinalIgnoreCase) && senderRole != "Admin")
                {
                    // Generate AI reply in background
                    var botReply = _chatBotService.GetAutomaticResponse(request.Message);
                    
                    await connection.ExecuteAsync(
                        "sp_ChatMessage_Insert",
                        new
                        {
                            SenderId = "AI_Chatbot",
                            SenderName = "दिंडोरी प्रणीत सहाय्यक (AI Bot)",
                            ReceiverId = senderId,
                            Message = botReply
                        },
                        commandType: CommandType.StoredProcedure
                    );
                }

                return Ok(new { success = true, message = "Message sent successfully." });
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"Error sending chat message: {ex.Message}");
                return StatusCode(500, new { success = false, message = "An internal error occurred." });
            }
        }

        [HttpGet("history/{receiverId}")]
        public async Task<IActionResult> GetChatHistory(string receiverId)
        {
            var senderId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(senderId)) return Unauthorized();

            // Support admins can view any user's chat with Admin, others view their own
            string user1 = senderId;
            string user2 = receiverId;

            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            try
            {
                var list = await connection.QueryAsync<dynamic>(
                    "sp_ChatMessage_GetHistory",
                    new { UserId1 = user1, UserId2 = user2 },
                    commandType: CommandType.StoredProcedure
                );

                // Mark messages from receiver as Read
                await connection.ExecuteAsync(
                    "UPDATE ChatMessages SET IsRead = 1 WHERE SenderId = @ReceiverId AND ReceiverId = @SenderId",
                    new { ReceiverId = receiverId, SenderId = senderId }
                );

                return Ok(new { success = true, data = list });
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"Error fetching chat history: {ex.Message}");
                return StatusCode(500, new { success = false, message = "Failed to load chat history." });
            }
        }

        [HttpGet("active-chats")]
        [Authorize(Roles = "Admin")] // Restricted to Admin
        public async Task<IActionResult> GetActiveChats()
        {
            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            try
            {
                // Fetch distinct users who have interacted with 'Admin'
                var list = await connection.QueryAsync<dynamic>(
                    @"SELECT DISTINCT CM.SenderId AS userId, CM.SenderName AS userName,
                        (SELECT TOP 1 Message FROM ChatMessages WHERE SenderId = CM.SenderId ORDER BY CreatedAt DESC) AS lastMessage,
                        (SELECT TOP 1 CreatedAt FROM ChatMessages WHERE SenderId = CM.SenderId ORDER BY CreatedAt DESC) AS lastMessageTime
                      FROM ChatMessages CM 
                      WHERE CM.ReceiverId = 'Admin' AND CM.SenderId != 'AI_Chatbot'
                      ORDER BY lastMessageTime DESC"
                );

                return Ok(new { success = true, data = list });
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"Error fetching active support chats: {ex.Message}");
                return StatusCode(500, new { success = false, message = "Failed to fetch active chats." });
            }
        }
    }

    public class SendMessageRequest
    {
        public string ReceiverId { get; set; } = string.Empty;
        public string Message { get; set; } = string.Empty;
    }
}
