using System;
using System.Data;
using System.Net.Http;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using Microsoft.Data.SqlClient;
using Dapper;

namespace DindoriPranitAPI.Services
{
    public class QueueProcessorService : BackgroundService
    {
        private readonly IServiceProvider _serviceProvider;
        private readonly string _connectionString;
        private readonly HttpClient _httpClient;

        public QueueProcessorService(IServiceProvider serviceProvider, IConfiguration configuration)
        {
            _serviceProvider = serviceProvider;
            _connectionString = configuration.GetConnectionString("DefaultConnection") 
                ?? throw new InvalidOperationException("DefaultConnection connection string not found.");
            _httpClient = new HttpClient { Timeout = TimeSpan.FromSeconds(15) };
        }

        protected override async Task ExecuteAsync(CancellationToken stoppingToken)
        {
            Console.WriteLine("Background Queue Processor Service started...");

            while (!stoppingToken.IsCancellationRequested)
            {
                try
                {
                    await ProcessQueuesAsync();
                }
                catch (Exception ex)
                {
                    Console.Error.WriteLine($"Error in background queue loop: {ex.Message}");
                }

                await Task.Delay(10000, stoppingToken); // Poll every 10 seconds
            }
        }

        private async Task ProcessQueuesAsync()
        {
            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            using var scope = _serviceProvider.CreateScope();
            var fcmService = scope.ServiceProvider.GetRequiredService<FcmService>();
            var whatsAppService = scope.ServiceProvider.GetRequiredService<WhatsAppService>();
            var emailService = scope.ServiceProvider.GetRequiredService<EmailService>();

            // 1. Process Webhooks Queue
            var pendingWebhooks = await connection.QueryAsync<dynamic>(
                @"SELECT Id, Payload, TargetUrl, RetryCount 
                  FROM WebhookQueue 
                  WHERE Status IN ('Pending', 'Failed') 
                    AND RetryCount < 3 
                    AND NextAttemptAt <= GETUTCDATE()"
            );

            foreach (var wh in pendingWebhooks)
            {
                int id = wh.Id;
                string payload = wh.Payload;
                string url = wh.TargetUrl;
                int retryCount = wh.RetryCount;

                try
                {
                    // Mark as Processing
                    await connection.ExecuteAsync("UPDATE WebhookQueue SET Status = 'Processing' WHERE Id = @Id", new { Id = id });

                    var content = new StringContent(payload, Encoding.UTF8, "application/json");
                    var response = await _httpClient.PostAsync(url, content);

                    if (response.IsSuccessStatusCode)
                    {
                        await connection.ExecuteAsync("UPDATE WebhookQueue SET Status = 'Sent', ErrorMessage = NULL WHERE Id = @Id", new { Id = id });
                    }
                    else
                    {
                        string statusError = $"HTTP {(int)response.StatusCode}: {response.ReasonPhrase}";
                        await HandleWebhookFailureAsync(connection, id, retryCount, statusError);
                    }
                }
                catch (Exception ex)
                {
                    await HandleWebhookFailureAsync(connection, id, retryCount, ex.Message);
                }
            }

            // 2. Process FCM Notifications Queue
            var pendingFcms = await connection.QueryAsync<dynamic>(
                @"SELECT Id, FcmToken, Title, Body, RetryCount 
                  FROM FcmQueue 
                  WHERE Status IN ('Pending', 'Failed') 
                    AND RetryCount < 3 
                    AND NextAttemptAt <= GETUTCDATE()"
            );

            foreach (var fcm in pendingFcms)
            {
                int id = fcm.Id;
                string token = fcm.FcmToken;
                string title = fcm.Title;
                string body = fcm.Body;
                int retryCount = fcm.RetryCount;

                try
                {
                    await connection.ExecuteAsync("UPDATE FcmQueue SET Status = 'Processing' WHERE Id = @Id", new { Id = id });

                    bool success = await fcmService.SendNotificationAsync(token, title, body);
                    if (success)
                    {
                        await connection.ExecuteAsync("UPDATE FcmQueue SET Status = 'Sent', ErrorMessage = NULL WHERE Id = @Id", new { Id = id });
                    }
                    else
                    {
                        await HandleFcmFailureAsync(connection, id, retryCount, "Firebase Dispatch Failed.");
                    }
                }
                catch (Exception ex)
                {
                    await HandleFcmFailureAsync(connection, id, retryCount, ex.Message);
                }
            }

            // 3. Process SMS / WhatsApp Queue
            var pendingSms = await connection.QueryAsync<dynamic>(
                @"SELECT Id, MobileNumber, Message, RetryCount 
                  FROM SmsQueue 
                  WHERE Status IN ('Pending', 'Failed') 
                    AND RetryCount < 3 
                    AND NextAttemptAt <= GETUTCDATE()"
            );

            foreach (var sms in pendingSms)
            {
                int id = sms.Id;
                string mobile = sms.MobileNumber;
                string message = sms.Message;
                int retryCount = sms.RetryCount;

                try
                {
                    await connection.ExecuteAsync("UPDATE SmsQueue SET Status = 'Processing' WHERE Id = @Id", new { Id = id });

                    bool success = await whatsAppService.SendNotificationAsync(mobile, message);
                    if (success)
                    {
                        await connection.ExecuteAsync("UPDATE SmsQueue SET Status = 'Sent', ErrorMessage = NULL WHERE Id = @Id", new { Id = id });
                    }
                    else
                    {
                        await HandleSmsFailureAsync(connection, id, retryCount, "WhatsApp Dispatch Failed.");
                    }
                }
                catch (Exception ex)
                {
                    await HandleSmsFailureAsync(connection, id, retryCount, ex.Message);
                }
            }

            // 4. Process Emails Queue
            var pendingEmails = await connection.QueryAsync<dynamic>(
                @"SELECT Id, ToEmail, Subject, Body, RetryCount 
                  FROM EmailQueue 
                  WHERE Status IN ('Pending', 'Failed') 
                    AND RetryCount < 3 
                    AND NextAttemptAt <= GETUTCDATE()"
            );

            foreach (var email in pendingEmails)
            {
                int id = email.Id;
                string toEmail = email.ToEmail;
                string subject = email.Subject;
                string body = email.Body;
                int retryCount = email.RetryCount;

                try
                {
                    await connection.ExecuteAsync("UPDATE EmailQueue SET Status = 'Processing' WHERE Id = @Id", new { Id = id });

                    bool success = await emailService.SendEmailAsync(toEmail, subject, body);
                    if (success)
                    {
                        await connection.ExecuteAsync("UPDATE EmailQueue SET Status = 'Sent', ErrorMessage = NULL WHERE Id = @Id", new { Id = id });
                    }
                    else
                    {
                        await HandleEmailFailureAsync(connection, id, retryCount, "SMTP Email Dispatch Failed.");
                    }
                }
                catch (Exception ex)
                {
                    await HandleEmailFailureAsync(connection, id, retryCount, ex.Message);
                }
            }
        }

        private async Task HandleWebhookFailureAsync(SqlConnection connection, int id, int retryCount, string error)
        {
            int newRetry = retryCount + 1;
            string newStatus = newRetry >= 3 ? "Failed" : "Failed";
            int waitSecs = 30 * (int)Math.Pow(2, newRetry);
            DateTime nextAttempt = DateTime.UtcNow.AddSeconds(waitSecs);

            await connection.ExecuteAsync(
                @"UPDATE WebhookQueue 
                  SET Status = @Status, RetryCount = @Retry, ErrorMessage = @Error, NextAttemptAt = @Next 
                  WHERE Id = @Id",
                new { Status = newStatus, Retry = newRetry, Error = error, Next = nextAttempt, Id = id }
            );
        }

        private async Task HandleFcmFailureAsync(SqlConnection connection, int id, int retryCount, string error)
        {
            int newRetry = retryCount + 1;
            int waitSecs = 30 * (int)Math.Pow(2, newRetry);
            DateTime nextAttempt = DateTime.UtcNow.AddSeconds(waitSecs);

            await connection.ExecuteAsync(
                @"UPDATE FcmQueue 
                  SET Status = 'Failed', RetryCount = @Retry, ErrorMessage = @Error, NextAttemptAt = @Next 
                  WHERE Id = @Id",
                new { Retry = newRetry, Error = error, Next = nextAttempt, Id = id }
            );
        }

        private async Task HandleSmsFailureAsync(SqlConnection connection, int id, int retryCount, string error)
        {
            int newRetry = retryCount + 1;
            int waitSecs = 30 * (int)Math.Pow(2, newRetry);
            DateTime nextAttempt = DateTime.UtcNow.AddSeconds(waitSecs);

            await connection.ExecuteAsync(
                @"UPDATE SmsQueue 
                  SET Status = 'Failed', RetryCount = @Retry, ErrorMessage = @Error, NextAttemptAt = @Next 
                  WHERE Id = @Id",
                new { Retry = newRetry, Error = error, Next = nextAttempt, Id = id }
            );
        }

        private async Task HandleEmailFailureAsync(SqlConnection connection, int id, int retryCount, string error)
        {
            int newRetry = retryCount + 1;
            int waitSecs = 30 * (int)Math.Pow(2, newRetry);
            DateTime nextAttempt = DateTime.UtcNow.AddSeconds(waitSecs);

            await connection.ExecuteAsync(
                @"UPDATE EmailQueue 
                  SET Status = 'Failed', RetryCount = @Retry, ErrorMessage = @Error, NextAttemptAt = @Next 
                  WHERE Id = @Id",
                new { Retry = newRetry, Error = error, Next = nextAttempt, Id = id }
            );
        }
    }
}
