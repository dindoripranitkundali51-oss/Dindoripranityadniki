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
    [Authorize(Roles = "Admin")] // Restricted to Admin role only
    public class AdminController : ControllerBase
    {
        private readonly IConfiguration _configuration;
        private readonly string _connectionString;

        public AdminController(IConfiguration configuration)
        {
            _configuration = configuration;
            _connectionString = _configuration.GetConnectionString("DefaultConnection") 
                ?? throw new InvalidOperationException("DefaultConnection connection string not found.");
        }

        [HttpPut("users/{uid}/status")]
        public async Task<IActionResult> ManageUserStatus(string uid, [FromBody] ManageStatusRequest request)
        {
            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            await connection.ExecuteAsync(
                "sp_User_UpdateStatus",
                new { Uid = uid, Status = request.Status },
                commandType: CommandType.StoredProcedure
            );

            // Log action in audit log
            await LogAdminAction(connection, "MANAGE_USER_STATUS", uid, $"Updated status to {request.Status}");

            return Ok(new { success = true, uid, status = request.Status });
        }

        [HttpPut("experts/{uid}/status")]
        public async Task<IActionResult> ManageExpertStatus(string uid, [FromBody] ManageStatusRequest request)
        {
            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            await connection.ExecuteAsync(
                "sp_Guruji_UpdateStatus",
                new { Uid = uid, Status = request.Status },
                commandType: CommandType.StoredProcedure
            );

            // Log action in audit log
            await LogAdminAction(connection, "MANAGE_EXPERT_STATUS", uid, $"Updated expert status to {request.Status}");

            return Ok(new { success = true, uid, status = request.Status });
        }

        [HttpGet("dashboard")]
        public async Task<IActionResult> GetDashboardStats()
        {
            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            var totalUsers = await connection.ExecuteScalarAsync<int>("SELECT COUNT(*) FROM Users");
            var totalExperts = await connection.ExecuteScalarAsync<int>("SELECT COUNT(*) FROM Guruji");
            var pendingExperts = await connection.ExecuteScalarAsync<int>("SELECT COUNT(*) FROM Guruji WHERE Status = 'Pending'");
            var totalBookings = await connection.ExecuteScalarAsync<int>("SELECT COUNT(*) FROM Bookings");
            var pendingBookings = await connection.ExecuteScalarAsync<int>("SELECT COUNT(*) FROM Bookings WHERE Status = 'Pending'");
            var completedBookings = await connection.ExecuteScalarAsync<int>("SELECT COUNT(*) FROM Bookings WHERE Status = 'Completed'");
            
            var bookingsToday = await connection.ExecuteScalarAsync<int>("SELECT COUNT(*) FROM Bookings WHERE CAST(CreatedAt AS DATE) = CAST(GETUTCDATE() AS DATE)");
            var revenueToday = await connection.ExecuteScalarAsync<decimal>("SELECT ISNULL(SUM(Amount), 0) FROM Bookings WHERE PaymentStatus = 'Paid' AND CAST(CreatedAt AS DATE) = CAST(GETUTCDATE() AS DATE)");

            var latestBookings = await connection.QueryAsync<dynamic>(
                "SELECT TOP 5 Id AS id, PoojaId AS poojaId, ContactName AS contactName, Status AS status FROM Bookings ORDER BY CreatedAt DESC"
            );

            var pendingRequests = await connection.QueryAsync<dynamic>(
                "SELECT TOP 5 Uid AS id, FullName AS fullName, Mobile AS mobile, CreatedAt AS createdAt FROM Guruji WHERE Status = 'Pending' ORDER BY CreatedAt DESC"
            );

            var financialSummary = await connection.QueryFirstOrDefaultAsync<dynamic>(
                "SELECT SUM(Amount) AS TotalRevenue, SUM(GurujiShare) AS TotalExpertShare, SUM(TrustShare) AS TotalTrustShare FROM Bookings WHERE PaymentStatus = 'Paid'"
            );

            return Ok(new
            {
                success = true,
                stats = new
                {
                    totalUsers,
                    totalExperts,
                    pendingExperts,
                    totalBookings,
                    pendingBookings,
                    completedBookings,
                    bookingsToday,
                    revenueToday,
                    totalRevenue = financialSummary?.TotalRevenue ?? 0.00,
                    totalExpertShare = financialSummary?.TotalExpertShare ?? 0.00,
                    totalTrustShare = financialSummary?.TotalTrustShare ?? 0.00
                },
                latestBookings,
                pendingRequests
            });
        }

        [HttpGet("users")]
        public async Task<IActionResult> GetUsers([FromQuery] int? page = null, [FromQuery] int? pageSize = null)
        {
            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            if (page.HasValue || pageSize.HasValue)
            {
                int p = page ?? 1;
                int ps = pageSize ?? 20;
                if (p < 1) p = 1;
                if (ps < 1 || ps > 100) ps = 20;
                int offset = (p - 1) * ps;

                var totalCount = await connection.ExecuteScalarAsync<int>("SELECT COUNT(*) FROM Users");
                var users = await connection.QueryAsync<dynamic>(
                    @"SELECT Uid AS id, FullName AS fullName, Mobile AS mobile, Email AS email, Address AS address, District AS district, Pincode AS pincode, Status AS status, CreatedAt AS createdAt 
                      FROM Users 
                      ORDER BY CreatedAt DESC 
                      OFFSET @Offset ROWS FETCH NEXT @PageSize ROWS ONLY",
                    new { Offset = offset, PageSize = ps }
                );

                return Ok(new { total = totalCount, page = p, pageSize = ps, rows = users });
            }
            else
            {
                var users = await connection.QueryAsync<dynamic>(
                    "SELECT Uid AS id, FullName AS fullName, Mobile AS mobile, Email AS email, Address AS address, District AS district, Pincode AS pincode, Status AS status, CreatedAt AS createdAt FROM Users ORDER BY CreatedAt DESC"
                );
                return Ok(users);
            }
        }

        [HttpGet("experts")]
        public async Task<IActionResult> GetExperts([FromQuery] int? page = null, [FromQuery] int? pageSize = null)
        {
            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            if (page.HasValue || pageSize.HasValue)
            {
                int p = page ?? 1;
                int ps = pageSize ?? 20;
                if (p < 1) p = 1;
                if (ps < 1 || ps > 100) ps = 20;
                int offset = (p - 1) * ps;

                var totalCount = await connection.ExecuteScalarAsync<int>("SELECT COUNT(*) FROM Guruji");
                var experts = await connection.QueryAsync<dynamic>(
                    @"SELECT Uid AS id, FullName AS fullName, Mobile AS mobile, Email AS email, Address AS address, District AS district, Pincode AS pincode, Status AS status, ExpertType AS expertType, WalletBalance AS walletBalance, TotalEarnings AS totalEarnings, PendingWithdrawal AS pendingWithdrawal, IsAvailable AS isAvailable, CreatedAt AS createdAt 
                      FROM Guruji 
                      ORDER BY CreatedAt DESC 
                      OFFSET @Offset ROWS FETCH NEXT @PageSize ROWS ONLY",
                    new { Offset = offset, PageSize = ps }
                );

                return Ok(new { total = totalCount, page = p, pageSize = ps, rows = experts });
            }
            else
            {
                var experts = await connection.QueryAsync<dynamic>(
                    "SELECT Uid AS id, FullName AS fullName, Mobile AS mobile, Email AS email, Address AS address, District AS district, Pincode AS pincode, Status AS status, ExpertType AS expertType, WalletBalance AS walletBalance, TotalEarnings AS totalEarnings, PendingWithdrawal AS pendingWithdrawal, IsAvailable AS isAvailable, CreatedAt AS createdAt FROM Guruji ORDER BY CreatedAt DESC"
                );
                return Ok(experts);
            }
        }

        [HttpGet("bookings")]
        public async Task<IActionResult> GetBookings([FromQuery] int? page = null, [FromQuery] int? pageSize = null)
        {
            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            if (page.HasValue || pageSize.HasValue)
            {
                int p = page ?? 1;
                int ps = pageSize ?? 20;
                if (p < 1) p = 1;
                if (ps < 1 || ps > 100) ps = 20;
                int offset = (p - 1) * ps;

                var totalCount = await connection.ExecuteScalarAsync<int>("SELECT COUNT(*) FROM Bookings");
                var bookings = await connection.QueryAsync<dynamic>(
                    @"SELECT Id AS id, DisplayId AS displayId, UserId AS userId, GurujiId AS gurujiId, PoojaId AS poojaId, Date AS date, Status AS status, PaymentStatus AS paymentStatus, Amount AS amount, ContactName AS contactName, ContactPhone AS contactPhone, Address AS address, District AS district, Pincode AS pincode, CreatedAt AS createdAt 
                      FROM Bookings 
                      ORDER BY CreatedAt DESC 
                      OFFSET @Offset ROWS FETCH NEXT @PageSize ROWS ONLY",
                    new { Offset = offset, PageSize = ps }
                );

                return Ok(new { total = totalCount, page = p, pageSize = ps, rows = bookings });
            }
            else
            {
                var bookings = await connection.QueryAsync<dynamic>(
                    "SELECT Id AS id, DisplayId AS displayId, UserId AS userId, GurujiId AS gurujiId, PoojaId AS poojaId, Date AS date, Status AS status, PaymentStatus AS paymentStatus, Amount AS amount, ContactName AS contactName, ContactPhone AS contactPhone, Address AS address, District AS district, Pincode AS pincode, CreatedAt AS createdAt FROM Bookings ORDER BY CreatedAt DESC"
                );
                return Ok(bookings);
            }
        }

        [HttpGet("payouts")]
        public async Task<IActionResult> GetPayouts([FromQuery] int? page = null, [FromQuery] int? pageSize = null)
        {
            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            if (page.HasValue || pageSize.HasValue)
            {
                int p = page ?? 1;
                int ps = pageSize ?? 20;
                if (p < 1) p = 1;
                if (ps < 1 || ps > 100) ps = 20;
                int offset = (p - 1) * ps;

                var totalCount = await connection.ExecuteScalarAsync<int>("SELECT COUNT(*) FROM WithdrawalRequests");
                var payouts = await connection.QueryAsync<dynamic>(
                    @"SELECT Id AS id, GurujiId AS gurujiId, Amount AS amount, BankAccount AS bankAccount, IFSC AS ifsc, UPI AS upi, Status AS status, SettlementRef AS settlementRef, CreatedAt AS createdAt 
                      FROM WithdrawalRequests 
                      ORDER BY CreatedAt DESC 
                      OFFSET @Offset ROWS FETCH NEXT @PageSize ROWS ONLY",
                    new { Offset = offset, PageSize = ps }
                );

                return Ok(new { total = totalCount, page = p, pageSize = ps, rows = payouts });
            }
            else
            {
                var payouts = await connection.QueryAsync<dynamic>(
                    "SELECT Id AS id, GurujiId AS gurujiId, Amount AS amount, BankAccount AS bankAccount, IFSC AS ifsc, UPI AS upi, Status AS status, SettlementRef AS settlementRef, CreatedAt AS createdAt FROM WithdrawalRequests ORDER BY CreatedAt DESC"
                );
                return Ok(payouts);
            }
        }

        [HttpGet("audit-logs")]
        public async Task<IActionResult> GetAuditLogs([FromQuery] int? page = null, [FromQuery] int? pageSize = null)
        {
            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            if (page.HasValue || pageSize.HasValue)
            {
                int p = page ?? 1;
                int ps = pageSize ?? 20;
                if (p < 1) p = 1;
                if (ps < 1 || ps > 100) ps = 20;
                int offset = (p - 1) * ps;

                var totalCount = await connection.ExecuteScalarAsync<int>("SELECT COUNT(*) FROM AuditLogs");
                var logs = await connection.QueryAsync<dynamic>(
                    @"SELECT Id AS id, AdminId AS adminId, AdminEmail AS adminEmail, AdminName AS adminName, Action AS action, Module AS module, TargetId AS targetId, Details AS details, Status AS status, CreatedAt AS createdAt 
                      FROM AuditLogs 
                      ORDER BY CreatedAt DESC 
                      OFFSET @Offset ROWS FETCH NEXT @PageSize ROWS ONLY",
                    new { Offset = offset, PageSize = ps }
                );

                return Ok(new { total = totalCount, page = p, pageSize = ps, rows = logs });
            }
            else
            {
                var logs = await connection.QueryAsync<dynamic>(
                    "SELECT Id AS id, AdminId AS adminId, AdminEmail AS adminEmail, AdminName AS adminName, Action AS action, Module AS module, TargetId AS targetId, Details AS details, Status AS status, CreatedAt AS createdAt FROM AuditLogs ORDER BY CreatedAt DESC"
                );
                return Ok(logs);
            }
        }

        [HttpGet("settings")]
        public async Task<IActionResult> GetSettings()
        {
            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            var systemConfigStr = await connection.QueryFirstOrDefaultAsync<string>(
                "SELECT SettingValue FROM SystemSettings WHERE SettingKey = 'system_config'"
            );
            var uiSettingsStr = await connection.QueryFirstOrDefaultAsync<string>(
                "SELECT SettingValue FROM SystemSettings WHERE SettingKey = 'ui_settings'"
            );

            return Ok(new
            {
                success = true,
                config = string.IsNullOrEmpty(systemConfigStr) ? null : System.Text.Json.JsonSerializer.Deserialize<object>(systemConfigStr),
                uiSettings = string.IsNullOrEmpty(uiSettingsStr) ? null : System.Text.Json.JsonSerializer.Deserialize<object>(uiSettingsStr)
            });
        }

        [HttpPut("settings")]
        public async Task<IActionResult> UpdateSettings([FromBody] UpdateSettingsRequest request)
        {
            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            if (request.Config != null)
            {
                var systemConfigStr = System.Text.Json.JsonSerializer.Serialize(request.Config);
                await connection.ExecuteAsync(
                    "EXEC sp_Settings_Update @Key = 'system_config', @Value = @Value",
                    new { Value = systemConfigStr }
                );
            }

            if (request.UiSettings != null)
            {
                var uiSettingsStr = System.Text.Json.JsonSerializer.Serialize(request.UiSettings);
                await connection.ExecuteAsync(
                    "EXEC sp_Settings_Update @Key = 'ui_settings', @Value = @Value",
                    new { Value = uiSettingsStr }
                );
            }

            await LogAdminAction(connection, "UPDATE_SYSTEM_SETTINGS", "System", "Updated system and UI configuration");

            return Ok(new { success = true, message = "Settings updated successfully." });
        }

        [HttpGet("poojas")]
        public async Task<IActionResult> GetPoojas()
        {
            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            var poojas = await connection.QueryAsync<dynamic>(
                "SELECT Id AS id, Name AS name, Category AS category, BasePrice AS basePrice, SevaType AS sevaType, Status AS status FROM Poojas WHERE Status <> 'Deleted' ORDER BY Category, Name"
            );

            return Ok(poojas);
        }

        [HttpPost("poojas")]
        public async Task<IActionResult> CreatePooja([FromBody] CreatePoojaRequest request)
        {
            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            await connection.ExecuteAsync(
                "sp_Pooja_Insert",
                new
                {
                    Id = request.Id,
                    Name = request.Name,
                    Category = request.Category,
                    BasePrice = request.BasePrice,
                    SevaType = request.SevaType ?? "Yadnyiki",
                    Status = request.Status ?? "Active"
                },
                commandType: CommandType.StoredProcedure
            );

            await LogAdminAction(connection, "CREATE_POOJA", request.Id, $"Created pooja {request.Name}");

            return Ok(new { success = true, id = request.Id });
        }

        [HttpPut("poojas/{id}")]
        public async Task<IActionResult> UpdatePooja(string id, [FromBody] UpdatePoojaRequest request)
        {
            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            await connection.ExecuteAsync(
                "sp_Pooja_Update",
                new
                {
                    Id = id,
                    Name = request.Name,
                    Category = request.Category,
                    BasePrice = request.BasePrice,
                    SevaType = request.SevaType ?? "Yadnyiki",
                    Status = request.Status ?? "Active"
                },
                commandType: CommandType.StoredProcedure
            );

            await LogAdminAction(connection, "UPDATE_POOJA", id, $"Updated pooja {request.Name}");

            return Ok(new { success = true, id });
        }

        [HttpDelete("poojas/{id}")]
        public async Task<IActionResult> DeletePooja(string id)
        {
            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            await connection.ExecuteAsync(
                "sp_Pooja_Delete",
                new { Id = id },
                commandType: CommandType.StoredProcedure
            );

            await LogAdminAction(connection, "DELETE_POOJA", id, $"Soft deleted pooja {id}");

            return Ok(new { success = true, id });
        }

        [HttpGet("support/tickets")]
        public async Task<IActionResult> GetSupportTickets()
        {
            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            var tickets = await connection.QueryAsync<dynamic>(
                "sp_SupportTicket_GetAll",
                commandType: CommandType.StoredProcedure
            );

            return Ok(tickets);
        }

        [HttpPut("support/tickets/{id}/status")]
        public async Task<IActionResult> UpdateSupportTicketStatus(string id, [FromBody] ManageStatusRequest request)
        {
            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            await connection.ExecuteAsync(
                "sp_SupportTicket_UpdateStatus",
                new { Id = id, Status = request.Status },
                commandType: CommandType.StoredProcedure
            );

            await LogAdminAction(connection, "MANAGE_SUPPORT_TICKET_STATUS", id, $"Updated status to {request.Status}");

            return Ok(new { success = true, id, status = request.Status });
        }

        [HttpPost("logWebAdminClientError")]
        public async Task<IActionResult> LogWebAdminClientError([FromBody] LogWebAdminClientErrorRequest request)
        {
            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            await connection.ExecuteAsync(
                @"INSERT INTO AdminClientErrors (Source, Level, Message, Page, Stack, Metadata, AdminId) 
                  VALUES (@Source, @Level, @Message, @Page, @Stack, @Metadata, @AdminId)",
                new
                {
                    Source = request.Source ?? "web-admin",
                    Level = request.Level ?? "error",
                    Message = request.Message,
                    Page = request.Page,
                    Stack = request.Stack,
                    Metadata = request.Metadata != null ? System.Text.Json.JsonSerializer.Serialize(request.Metadata) : null,
                    AdminId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value
                }
            );

            return Ok(new { success = true });
        }

        [HttpGet("faqs")]
        public async Task<IActionResult> GetFaqs()
        {
            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            var faqs = await connection.QueryAsync<dynamic>(
                "SELECT Id AS id, Question AS question, Answer AS answer, Tags AS tagsString, Status AS status, CreatedAt AS createdAt FROM FAQs ORDER BY CreatedAt DESC"
            );

            var result = faqs.Select(f => {
                string tagsStr = f.tagsString;
                string[] tags = string.IsNullOrEmpty(tagsStr) ? Array.Empty<string>() : tagsStr.Split(',').Select(t => t.Trim()).ToArray();
                return new {
                    id = f.id,
                    question = f.question,
                    answer = f.answer,
                    tags = tags,
                    status = f.status,
                    createdAt = f.createdAt
                };
            });

            return Ok(result);
        }

        [HttpPost("faqs")]
        public async Task<IActionResult> CreateFaq([FromBody] CreateFaqRequest request)
        {
            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            var tagsStr = request.Tags != null ? string.Join(",", request.Tags) : null;
            var faqId = await connection.QuerySingleAsync<int>(
                "sp_FAQ_Insert",
                new
                {
                    Question = request.Question,
                    Answer = request.Answer,
                    Tags = tagsStr,
                    Status = request.Status ?? "Published"
                },
                commandType: CommandType.StoredProcedure
            );

            await LogAdminAction(connection, "CREATE_FAQ", faqId.ToString(), $"Created FAQ: {request.Question}");

            return Ok(new { success = true, id = faqId });
        }

        [HttpPut("faqs/{id}")]
        public async Task<IActionResult> UpdateFaq(int id, [FromBody] UpdateFaqRequest request)
        {
            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            var tagsStr = request.Tags != null ? string.Join(",", request.Tags) : null;
            await connection.ExecuteAsync(
                "sp_FAQ_Update",
                new
                {
                    Id = id,
                    Question = request.Question,
                    Answer = request.Answer,
                    Tags = tagsStr,
                    Status = request.Status
                },
                commandType: CommandType.StoredProcedure
            );

            await LogAdminAction(connection, "UPDATE_FAQ", id.ToString(), $"Updated FAQ ID {id}");

            return Ok(new { success = true, id });
        }

        [HttpDelete("faqs/{id}")]
        public async Task<IActionResult> DeleteFaq(int id)
        {
            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            await connection.ExecuteAsync(
                "sp_FAQ_Delete",
                new { Id = id },
                commandType: CommandType.StoredProcedure
            );

            await LogAdminAction(connection, "DELETE_FAQ", id.ToString(), $"Deleted FAQ ID {id}");

            return Ok(new { success = true, id });
        }

        [HttpGet("languages/{lang}")]
        public async Task<IActionResult> GetLanguageStrings(string lang)
        {
            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            var settingKey = $"lang_{lang.ToLower()}";
            var stringsJson = await connection.QueryFirstOrDefaultAsync<string>(
                "SELECT SettingValue FROM SystemSettings WHERE SettingKey = @Key",
                new { Key = settingKey }
            );

            object strings = string.IsNullOrEmpty(stringsJson) 
                ? new Dictionary<string, string>() 
                : System.Text.Json.JsonSerializer.Deserialize<object>(stringsJson)!;

            return Ok(new { success = true, strings = strings });
        }

        [HttpPut("languages/{lang}")]
        public async Task<IActionResult> SaveLanguageStrings(string lang, [FromBody] Dictionary<string, string> requestBody)
        {
            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            var settingKey = $"lang_{lang.ToLower()}";
            var stringsJson = System.Text.Json.JsonSerializer.Serialize(requestBody);

            await connection.ExecuteAsync(
                "EXEC sp_Settings_Update @Key = @Key, @Value = @Value",
                new { Key = settingKey, Value = stringsJson }
            );

            await LogAdminAction(connection, "UPDATE_APP_TRANSLATIONS", settingKey, $"Updated {lang} translation dictionary");

            return Ok(new { success = true });
        }

        [HttpGet("cms")]
        public async Task<IActionResult> GetAndroidCms()
        {
            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            var cmsJson = await connection.QueryFirstOrDefaultAsync<string>(
                "SELECT SettingValue FROM SystemSettings WHERE SettingKey = 'android_cms'"
            );

            object config = string.IsNullOrEmpty(cmsJson)
                ? new {
                    app_banner_en = "",
                    app_banner_mr = "",
                    booking_primary_cta_en = "Start Seva",
                    booking_primary_cta_mr = "सेवा सुरू करा",
                    support_whatsapp = "",
                    enable_feedback_prompt = true,
                    force_refresh_after_hours = 12
                  }
                : System.Text.Json.JsonSerializer.Deserialize<object>(cmsJson)!;

            return Ok(new { success = true, config = config });
        }

        [HttpPut("cms")]
        public async Task<IActionResult> SaveAndroidCms([FromBody] object config)
        {
            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            var cmsJson = System.Text.Json.JsonSerializer.Serialize(config);

            await connection.ExecuteAsync(
                "EXEC sp_Settings_Update @Key = 'android_cms', @Value = @Value",
                new { Value = cmsJson }
            );

            await LogAdminAction(connection, "UPDATE_ANDROID_CMS", "android_cms", "Updated Android CMS config copy");

            return Ok(new { success = true });
        }

        [HttpGet("legal/{doc}")]
        public async Task<IActionResult> GetLegalDoc(string doc)
        {
            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            var settingKey = $"legal_{doc.ToLower()}";
            var content = await connection.QueryFirstOrDefaultAsync<string>(
                "SELECT SettingValue FROM SystemSettings WHERE SettingKey = @Key",
                new { Key = settingKey }
            );

            return Ok(new { success = true, content = content ?? "" });
        }

        [HttpPut("legal/{doc}")]
        public async Task<IActionResult> SaveLegalDoc(string doc, [FromBody] UpdateLegalDocRequest request)
        {
            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            var settingKey = $"legal_{doc.ToLower()}";

            await connection.ExecuteAsync(
                "EXEC sp_Settings_Update @Key = @Key, @Value = @Value",
                new { Key = settingKey, Value = request.Content ?? "" }
            );

            await LogAdminAction(connection, "UPDATE_LEGAL_CONTENT", settingKey, $"Updated legal content for {doc}");

            return Ok(new { success = true });
        }

        private async Task LogAdminAction(SqlConnection connection, string action, string targetId, string details)
        {
            var adminId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value ?? "system";
            await connection.ExecuteAsync(
                "INSERT INTO AuditLogs (adminId, adminEmail, adminName, action, module, targetId, details, status) VALUES (@AdminId, 'admin@dindori.org', 'Admin Owner', @Action, 'AdminPanel', @TargetId, @Details, 'Success')",
                new { AdminId = adminId, Action = action, TargetId = targetId, Details = details }
            );
        }
    }

    public class ManageStatusRequest
    {
        public string Status { get; set; } = "Active";
    }

    public class UpdateSettingsRequest
    {
        public object? Config { get; set; }
        public object? UiSettings { get; set; }
    }

    public class CreatePoojaRequest
    {
        public string Id { get; set; } = string.Empty;
        public string Name { get; set; } = string.Empty;
        public string Category { get; set; } = string.Empty;
        public decimal BasePrice { get; set; }
        public string? SevaType { get; set; }
        public string? Status { get; set; }
    }

    public class UpdatePoojaRequest
    {
        public string Name { get; set; } = string.Empty;
        public string Category { get; set; } = string.Empty;
        public decimal BasePrice { get; set; }
        public string? SevaType { get; set; }
        public string? Status { get; set; }
    }

    public class LogWebAdminClientErrorRequest
    {
        public string? Source { get; set; }
        public string? Level { get; set; }
        public string Message { get; set; } = string.Empty;
        public string? Page { get; set; }
        public string? Stack { get; set; }
        public object? Metadata { get; set; }
    }

    public class CreateFaqRequest
    {
        public string Question { get; set; } = string.Empty;
        public string Answer { get; set; } = string.Empty;
        public string[]? Tags { get; set; }
        public string? Status { get; set; }
    }

    public class UpdateFaqRequest
    {
        public string Question { get; set; } = string.Empty;
        public string Answer { get; set; } = string.Empty;
        public string[]? Tags { get; set; }
        public string Status { get; set; } = "Published";
    }

    public class UpdateLegalDocRequest
    {
        public string? Content { get; set; }
    }
}
