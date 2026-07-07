using System;
using System.Data;
using System.Net.Http;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;
using Dapper;
using Microsoft.Data.SqlClient;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;

namespace DindoriPranitAPI.Services
{
    public class WebhookService
    {
        private readonly IConfiguration _configuration;
        private readonly ILogger<WebhookService> _logger;
        private readonly string _connectionString;
        private readonly HttpClient _httpClient;

        public WebhookService(IConfiguration configuration, ILogger<WebhookService> logger)
        {
            _configuration = configuration;
            _logger = logger;
            _connectionString = _configuration.GetConnectionString("DefaultConnection") 
                ?? throw new InvalidOperationException("DefaultConnection connection string not found.");
            _httpClient = new HttpClient();
            _httpClient.Timeout = TimeSpan.FromSeconds(10);
        }

        public async Task TriggerWebhookAsync(string eventType, object dataPayload)
        {
            try
            {
                using var connection = new SqlConnection(_connectionString);
                await connection.OpenAsync();

                // Fetch active webhooks
                var webhooks = await connection.QueryAsync<dynamic>(
                    "SELECT Url, Secret FROM WebhookSettings WHERE IsActive = 1"
                );

                var payload = new
                {
                    @event = eventType,
                    timestamp = DateTime.UtcNow,
                    data = dataPayload
                };

                var json = JsonSerializer.Serialize(payload);
                var content = new StringContent(json, Encoding.UTF8, "application/json");

                foreach (var webhook in webhooks)
                {
                    string url = webhook.Url.ToString();
                    string? secret = webhook.Secret?.ToString();

                    _ = Task.Run(async () =>
                    {
                        try
                        {
                            var request = new HttpRequestMessage(HttpMethod.Post, url);
                            request.Content = content;

                            if (!string.IsNullOrEmpty(secret))
                            {
                                // Simple signature header for verification
                                request.Headers.Add("X-Dindori-Signature", CalculateHmacSignature(json, secret));
                            }

                            _logger.LogInformation($"Dispatching Webhook {eventType} to {url}...");
                            var response = await _httpClient.SendAsync(request);
                            
                            if (response.IsSuccessStatusCode)
                            {
                                _logger.LogInformation($"Webhook {eventType} dispatched successfully to {url}.");
                            }
                            else
                            {
                                _logger.LogWarning($"Webhook {url} responded with error code: {response.StatusCode}");
                            }
                        }
                        catch (Exception ex)
                        {
                            _logger.LogError($"Error dispatching webhook to {url}: {ex.Message}");
                        }
                    });
                }
            }
            catch (Exception ex)
            {
                _logger.LogError($"Error in Webhook trigger system: {ex.Message}");
            }
        }

        private string CalculateHmacSignature(string payload, string secret)
        {
            using var hmac = new System.Security.Cryptography.HMACSHA256(Encoding.UTF8.GetBytes(secret));
            var hash = hmac.ComputeHash(Encoding.UTF8.GetBytes(payload));
            return Convert.ToBase64String(hash);
        }
    }
}
