using System;
using System.Net;
using System.Net.Mail;
using System.Threading.Tasks;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;

namespace DindoriPranitAPI.Services
{
    public class EmailService
    {
        private readonly IConfiguration _configuration;
        private readonly ILogger<EmailService> _logger;

        public EmailService(IConfiguration configuration, ILogger<EmailService> logger)
        {
            _configuration = configuration;
            _logger = logger;
        }

        public async Task<bool> SendEmailAsync(string toEmail, string subject, string body)
        {
            try
            {
                var host = _configuration["Smtp:Host"];
                var portStr = _configuration["Smtp:Port"] ?? "587";
                var username = _configuration["Smtp:Username"];
                var password = _configuration["Smtp:Password"];
                var fromEmail = _configuration["Smtp:FromEmail"] ?? "no-reply@dindoripranit.org";

                if (string.IsNullOrWhiteSpace(host) || string.IsNullOrWhiteSpace(username) || username == "smtp_username_placeholder")
                {
                    _logger.LogWarning($"SMTP Email dispatch is disabled. Local Log: To: {toEmail}, Subject: '{subject}', Body: '{body}'");
                    return true; // Mock success
                }

                int port = int.Parse(portStr);

                using var mailMessage = new MailMessage();
                mailMessage.From = new MailAddress(fromEmail);
                mailMessage.To.Add(new MailAddress(toEmail));
                mailMessage.Subject = subject;
                mailMessage.Body = body;
                mailMessage.IsBodyHtml = true;

                using var smtpClient = new SmtpClient(host, port);
                smtpClient.Credentials = new NetworkCredential(username, password);
                smtpClient.EnableSsl = true;

                await smtpClient.SendMailAsync(mailMessage);
                _logger.LogInformation($"Email sent successfully to {toEmail}.");
                return true;
            }
            catch (Exception ex)
            {
                _logger.LogError($"SMTP Email delivery failed: {ex.Message}");
                return false;
            }
        }
    }
}
