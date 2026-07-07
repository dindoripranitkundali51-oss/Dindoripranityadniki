using System.Text;
using System.Text.Json;
using Google.Apis.Auth.OAuth2;

namespace DindoriPranitAPI.Services
{
    public class FcmService
    {
        private readonly IConfiguration _configuration;
        private readonly HttpClient _httpClient;
        private readonly string _projectId;
        private readonly string _serviceAccountJsonPath;

        public FcmService(IConfiguration configuration)
        {
            _configuration = configuration;
            _httpClient = new HttpClient();
            _projectId = _configuration["Firebase:ProjectId"] ?? "dindori-pranit-yadnyiki";
            _serviceAccountJsonPath = _configuration["Firebase:ServiceAccountJsonPath"] ?? "service-account.json";
        }

        private async Task<string> GetAccessTokenAsync()
        {
            if (!File.Exists(_serviceAccountJsonPath))
            {
                // Fallback: If service account is not found, return empty token to allow graceful bypass in trial mode
                return string.Empty;
            }

#pragma warning disable CS0618
            GoogleCredential credential;
            using (var stream = new FileStream(_serviceAccountJsonPath, FileMode.Open, FileAccess.Read))
            {
                credential = GoogleCredential.FromStream(stream)
                    .CreateScoped("https://www.googleapis.com/auth/firebase.messaging");
            }
#pragma warning restore CS0618

            var token = await credential.UnderlyingCredential.GetAccessTokenForRequestAsync();
            return token;
        }

        public async Task<bool> SendNotificationAsync(string targetToken, string title, string body, Dictionary<string, string>? data = null)
        {
            if (string.IsNullOrWhiteSpace(targetToken)) return false;

            try
            {
                var accessToken = await GetAccessTokenAsync();
                if (string.IsNullOrEmpty(accessToken))
                {
                    Console.WriteLine("Firebase service account json not set up. Skipping FCM push notification dispatch.");
                    return false;
                }

                var url = $"https://fcm.googleapis.com/v1/projects/{_projectId}/messages:send";

                var payload = new
                {
                    message = new
                    {
                        token = targetToken,
                        notification = new
                        {
                            title = title,
                            body = body
                        },
                        data = data
                    }
                };

                var json = JsonSerializer.Serialize(payload);
                using var request = new HttpRequestMessage(HttpMethod.Post, url);
                request.Headers.Authorization = new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", accessToken);
                request.Content = new StringContent(json, Encoding.UTF8, "application/json");

                using var response = await _httpClient.SendAsync(request);
                return response.IsSuccessStatusCode;
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Error sending FCM notification: {ex.Message}");
                return false;
            }
        }
    }
}
