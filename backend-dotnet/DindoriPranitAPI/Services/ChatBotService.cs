using System;
using System.Collections.Generic;
using System.Data;
using Microsoft.Data.SqlClient;
using Dapper;
using Microsoft.Extensions.Configuration;

namespace DindoriPranitAPI.Services
{
    public class ChatBotService
    {
        private readonly string _connectionString;

        public ChatBotService(IConfiguration configuration)
        {
            _connectionString = configuration.GetConnectionString("DefaultConnection") 
                ?? throw new InvalidOperationException("DefaultConnection connection string not found.");
        }

        public string GetAutomaticResponse(string userMessage)
        {
            var text = userMessage.Trim().ToLower();

            try
            {
                using var connection = new SqlConnection(_connectionString);
                connection.Open();

                var faqs = connection.Query<FaqItem>(
                    "SELECT Question, Answer, Tags FROM FAQs WHERE Status = 'Published'"
                );

                foreach (var faq in faqs)
                {
                    if (text.Contains(faq.Question.ToLower()))
                    {
                        return faq.Answer;
                    }

                    if (!string.IsNullOrEmpty(faq.Tags))
                    {
                        var tags = faq.Tags.Split(',');
                        foreach (var tag in tags)
                        {
                            if (!string.IsNullOrEmpty(tag) && text.Contains(tag.Trim().ToLower()))
                            {
                                return faq.Answer;
                            }
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"ChatBotService database error: {ex.Message}");
            }

            // Default fallbacks based on language detection
            if (ContainsMarathiCharacters(text))
            {
                return "तुमची विनंती प्राप्त झाली आहे. लवकरच आमचा सपोर्ट प्रतिनिधी तुमच्याशी जोडला जाईल. धन्यवाद!";
            }

            return "Thank you for reaching out. We have received your query, and a support representative will connect with you shortly.";
        }

        private bool ContainsMarathiCharacters(string text)
        {
            foreach (char c in text)
            {
                if (c >= 0x0900 && c <= 0x097F)
                    return true;
            }
            return false;
        }

        private class FaqItem
        {
            public string Question { get; set; } = string.Empty;
            public string Answer { get; set; } = string.Empty;
            public string? Tags { get; set; }
        }
    }
}
