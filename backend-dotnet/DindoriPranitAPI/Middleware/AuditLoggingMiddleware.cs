using System;
using System.IO;
using System.Security.Claims;
using System.Text;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Http;
using Microsoft.Data.SqlClient;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;
using Dapper;
using System.Data;

namespace DindoriPranitAPI.Middleware
{
    public class AuditLoggingMiddleware
    {
        private readonly RequestDelegate _next;
        private readonly ILogger<AuditLoggingMiddleware> _logger;
        private readonly string _connectionString;

        public AuditLoggingMiddleware(RequestDelegate next, ILogger<AuditLoggingMiddleware> logger, IConfiguration configuration)
        {
            _next = next;
            _logger = logger;
            _connectionString = configuration.GetConnectionString("DefaultConnection") 
                ?? throw new InvalidOperationException("DefaultConnection connection string not found.");
        }

        public async Task InvokeAsync(HttpContext context)
        {
            var path = context.Request.Path.Value ?? "";

            // Audit log ONLY admin operations
            if (path.Contains("/api/v1/Admin", StringComparison.OrdinalIgnoreCase))
            {
                context.Request.EnableBuffering();

                var clientIp = GetClientIp(context);
                var adminId = context.User.FindFirst(ClaimTypes.NameIdentifier)?.Value ?? "Anonymous";
                var adminEmail = context.User.FindFirst(ClaimTypes.Email)?.Value ?? context.User.Identity?.Name ?? "Unknown";

                var requestBody = "";
                using (var reader = new StreamReader(context.Request.Body, Encoding.UTF8, true, 1024, true))
                {
                    requestBody = await reader.ReadToEndAsync();
                    context.Request.Body.Position = 0; // Reset stream position
                }

                var action = $"{context.Request.Method} {path}";
                var threatLevel = AssessThreat(path, requestBody, clientIp);

                // Insert advanced log into DB
                try
                {
                    using var dbConnection = new SqlConnection(_connectionString);
                    await dbConnection.OpenAsync();

                    await dbConnection.ExecuteAsync(
                        "sp_AuditLog_Insert_Advanced",
                        new
                        {
                            AdminId = adminId,
                            Action = action,
                            TargetId = "System",
                            Details = $"Admin ({adminEmail}) accessed {action}.",
                            ClientIp = clientIp,
                            RequestPayload = string.IsNullOrEmpty(requestBody) ? null : requestBody,
                            ThreatLevel = threatLevel
                        },
                        commandType: CommandType.StoredProcedure
                    );

                    // If high threat level, trigger an alert in AdminRiskEvents
                    if (threatLevel == "High" || threatLevel == "Critical")
                    {
                        _logger.LogWarning($"SECURITY ALERT: Potential intrusion detected from IP {clientIp}! Threat level: {threatLevel}");
                        await dbConnection.ExecuteAsync(
                            @"INSERT INTO AdminRiskEvents (Type, Severity, Status, Message, CreatedAt, UpdatedAt) 
                              VALUES (@Type, @Severity, 'Open', @Message, GETUTCDATE(), GETUTCDATE())",
                            new
                            {
                                Type = "INTRUSION_ALERT",
                                Severity = threatLevel.ToLower(),
                                Message = $"Suspicious admin activity on {action} from IP {clientIp}. Details: {threatLevel} threat level triggered."
                            }
                        );
                    }
                }
                catch (Exception ex)
                {
                    _logger.LogError($"Failed to write advanced audit log: {ex.Message}");
                }
            }

            await _next(context);
        }

        private string GetClientIp(HttpContext context)
        {
            var ip = context.Request.Headers["X-Forwarded-For"].ToString();
            if (string.IsNullOrEmpty(ip))
            {
                ip = context.Connection.RemoteIpAddress?.ToString() ?? "Unknown";
            }
            else
            {
                // Split proxies and pick the first IP
                ip = ip.Split(',')[0].Trim();
            }

            if (ip == "::1") ip = "127.0.0.1"; // Localhost mapping
            return ip;
        }

        private string AssessThreat(string path, string body, string ip)
        {
            // Simple Intrusion Detection Logic
            // 1. Detect Sql Injection patterns in payload
            var sqlInjectionPattern = new[] { "UNION SELECT", "OR '1'='1", "DROP TABLE", "--", "CAST(" };
            foreach (var pattern in sqlInjectionPattern)
            {
                if (body.Contains(pattern, StringComparison.OrdinalIgnoreCase))
                {
                    return "Critical";
                }
            }

            // 2. Accessing critical endpoints without a standard local address
            if (path.Contains("/backup", StringComparison.OrdinalIgnoreCase) || path.Contains("/settings", StringComparison.OrdinalIgnoreCase))
            {
                if (ip != "127.0.0.1" && !ip.StartsWith("192.168.") && !ip.StartsWith("10."))
                {
                    return "High"; // Potential access from outside local/trusted range
                }
            }

            return "Low";
        }
    }
}
