using System.IO;
using System.Text;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Http;
using Microsoft.Data.SqlClient;
using Dapper;

namespace DindoriPranitAPI.Middleware
{
    public class IdempotencyMiddleware
    {
        private readonly RequestDelegate _next;
        private readonly string _connectionString;

        public IdempotencyMiddleware(RequestDelegate next, IConfiguration configuration)
        {
            _next = next;
            _connectionString = configuration.GetConnectionString("DefaultConnection") 
                ?? throw new InvalidOperationException("DefaultConnection connection string not found.");
        }

        public async Task InvokeAsync(HttpContext context)
        {
            var method = context.Request.Method;
            if (method != "POST" && method != "PUT" && method != "DELETE")
            {
                await _next(context);
                return;
            }

            if (!context.Request.Headers.TryGetValue("Idempotency-Key", out var idempotencyKey) || 
                string.IsNullOrWhiteSpace(idempotencyKey))
            {
                await _next(context);
                return;
            }

            string key = idempotencyKey.ToString().Trim();

            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            // Check if request was already processed
            var cachedResponse = await connection.QueryFirstOrDefaultAsync<dynamic>(
                "SELECT ResponseBody, ResponseStatusCode FROM IdempotentRequests WHERE IdKey = @Key",
                new { Key = key }
            );

            if (cachedResponse != null)
            {
                context.Response.ContentType = "application/json";
                context.Response.StatusCode = (int)cachedResponse.ResponseStatusCode;
                context.Response.Headers.Add("X-Cache-Lookup", "Hit-Idempotent");
                
                string body = cachedResponse.ResponseBody;
                await context.Response.WriteAsync(body);
                return;
            }

            // Wrap the response body stream to capture the output
            var originalResponseBodyStream = context.Response.Body;
            using var responseBodyMemoryStream = new MemoryStream();
            context.Response.Body = responseBodyMemoryStream;

            try
            {
                await _next(context);

                // Read the response from MemoryStream
                context.Response.Body.Seek(0, SeekOrigin.Begin);
                string responseBody = await new StreamReader(context.Response.Body).ReadToEndAsync();
                context.Response.Body.Seek(0, SeekOrigin.Begin);

                // Save to Cache if it is a successful status or normal client error (e.g. status < 500)
                // We avoid caching internal server crashes (status >= 500) so client can safely retry
                if (context.Response.StatusCode < 500)
                {
                    try
                    {
                        await connection.ExecuteAsync(
                            "INSERT INTO IdempotentRequests (IdKey, ResponseBody, ResponseStatusCode) VALUES (@Key, @Body, @Status)",
                            new { Key = key, Body = responseBody, Status = context.Response.StatusCode }
                        );
                    }
                    catch (SqlException ex) when (ex.Number == 2627)
                    {
                        // Handle race condition: if another thread wrote it concurrently
                    }
                }

                // Copy the captured response back to the original response stream
                await responseBodyMemoryStream.CopyToAsync(originalResponseBodyStream);
            }
            finally
            {
                context.Response.Body = originalResponseBodyStream;
            }
        }
    }
}
