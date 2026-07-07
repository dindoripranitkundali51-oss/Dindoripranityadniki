using System;
using System.Net.Http;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;

namespace DindoriPranitAPI.Services
{
    public class WhatsAppService
    {
        private readonly IConfiguration _configuration;
        private readonly ILogger<WhatsAppService> _logger;
        private readonly HttpClient _httpClient;

        public WhatsAppService(IConfiguration configuration, ILogger<WhatsAppService> logger)
        {
            _configuration = configuration;
            _logger = logger;
            _httpClient = new HttpClient();
        }

        public async Task<bool> SendNotificationAsync(string toMobile, string message)
        {
            var apiKey = _configuration["WhatsApp:ApiKey"];
            var apiEndpoint = _configuration["WhatsApp:ApiEndpoint"] ?? "https://api.whatsapp.com/v1/messages";

            if (string.IsNullOrWhiteSpace(apiKey) || apiKey == "your_whatsapp_api_key_placeholder")
            {
                _logger.LogWarning($"WhatsApp API is disabled. Local Log: Target Mobile: {toMobile}, Message: '{message}'");
                return true; // Return true as mock success
            }

            try
            {
                _logger.LogInformation($"Sending WhatsApp alert to {toMobile}...");
                var payload = new
                {
                    messaging_product = "whatsapp",
                    to = toMobile,
                    type = "text",
                    text = new { body = message }
                };

                var request = new HttpRequestMessage(HttpMethod.Post, apiEndpoint);
                request.Headers.Add("Authorization", $"Bearer {apiKey}");
                request.Content = new StringContent(JsonSerializer.Serialize(payload), Encoding.UTF8, "application/json");

                var response = await _httpClient.SendAsync(request);
                if (response.IsSuccessStatusCode)
                {
                    _logger.LogInformation($"WhatsApp alert sent successfully to {toMobile}.");
                    return true;
                }

                var responseContent = await response.Content.ReadAsStringAsync();
                _logger.LogError($"Failed to send WhatsApp alert. Status: {response.StatusCode}, Response: {responseContent}");
                return false;
            }
            catch (Exception ex)
            {
                _logger.LogError($"Error in WhatsApp service delivery: {ex.Message}");
                return false;
            }
        }
    }
}
