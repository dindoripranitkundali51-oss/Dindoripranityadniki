using System.Data;
using System.Security.Claims;
using System.Security.Cryptography;
using System.Text;
using Dapper;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Data.SqlClient;

namespace DindoriPranitAPI.Controllers
{
    [ApiController]
    [Route("api/v1/[controller]")]
    [Authorize]
    public class PaymentController : ControllerBase
    {
        private readonly IConfiguration _configuration;
        private readonly string _connectionString;

        public PaymentController(IConfiguration configuration)
        {
            _configuration = configuration;
            _connectionString = _configuration.GetConnectionString("DefaultConnection") 
                ?? throw new InvalidOperationException("DefaultConnection connection string not found.");
        }

        [HttpPost("create-order")]
        public async Task<IActionResult> CreateOrder([FromBody] CreateOrderRequest request)
        {
            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            var booking = await connection.QueryFirstOrDefaultAsync<dynamic>(
                "SELECT * FROM Bookings WHERE Id = @Id",
                new { Id = request.BookingId }
            );

            if (booking == null) return NotFound(new { success = false, message = "Booking not found." });

            // Generate Razorpay Order ID Placeholder
            var razorpayOrderId = "order_" + Guid.NewGuid().ToString().Replace("-", "").Substring(0, 14);

            await connection.ExecuteAsync(
                "UPDATE Bookings SET RazorpayOrderId = @OrderId, PaymentStatus = 'OrderCreated', UpdatedAt = GETUTCDATE() WHERE Id = @BookingId",
                new { OrderId = razorpayOrderId, BookingId = request.BookingId }
            );

            return Ok(new { 
                success = true, 
                orderId = razorpayOrderId,
                amount = booking.Amount,
                keyId = _configuration["Razorpay:KeyId"] ?? "rzp_test_placeholder_key_id"
            });
        }

        [HttpPost("verify")]
        public async Task<IActionResult> VerifyPayment([FromBody] VerifyPaymentRequest request)
        {
            var actorId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(actorId)) return Unauthorized();

            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            string? dbOrderId = null;
            try
            {
                dbOrderId = await connection.QueryFirstOrDefaultAsync<string>(
                    "SELECT RazorpayOrderId FROM Bookings WHERE Id = @BookingId",
                    new { BookingId = request.BookingId }
                );
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"Error retrieving RazorpayOrderId: {ex}");
                return StatusCode(500, new { success = false, message = "An internal server error occurred while processing the payment." });
            }

            if (string.IsNullOrEmpty(dbOrderId))
            {
                return BadRequest(new { success = false, message = "Booking or associated Razorpay Order ID not found." });
            }

            // Verify Razorpay HMAC signature: HMAC-SHA256(orderId + "|" + paymentId, KeySecret)
            string keySecret = _configuration["Razorpay:KeySecret"] ?? throw new InvalidOperationException("Razorpay KeySecret is not configured.");
            string payload = $"{dbOrderId}|{request.RazorpayPaymentId}";
            
            bool isSignatureValid = VerifyRazorpaySignature(payload, request.RazorpaySignature, keySecret);

            if (!isSignatureValid)
            {
                return BadRequest(new { success = false, message = "Invalid payment signature verification." });
            }

            try
            {
                // Retrieve booking details to compute shares for split routing
                var bookingDetails = await connection.QueryFirstOrDefaultAsync<dynamic>(
                    "SELECT GurujiId, Amount, GurujiShare, TrustShare FROM Bookings WHERE Id = @BookingId",
                    new { BookingId = request.BookingId }
                );

                // Run SP to capture paid booking (splits commissions, credits wallet, unlocks dates)
                var result = await connection.QueryFirstOrDefaultAsync<dynamic>(
                    "sp_Payment_Capture",
                    new
                    {
                        BookingId = request.BookingId,
                        PaymentId = request.RazorpayPaymentId,
                        PaymentMethod = "razorpay_gateway",
                        ActorId = actorId
                    },
                    commandType: CommandType.StoredProcedure
                );

                if (bookingDetails != null)
                {
                    decimal gurujiShare = bookingDetails.GurujiShare ?? 0m;
                    decimal trustShare = bookingDetails.TrustShare ?? 0m;
                    decimal amount = bookingDetails.Amount ?? 0m;
                    string gurujiId = bookingDetails.GurujiId ?? "";

                    // Trigger Razorpay Split Route Transfer
                    await TriggerRazorpaySplitSettlementAsync(request.RazorpayPaymentId, amount, gurujiShare, trustShare, gurujiId);
                }

                return Ok(new { success = true, message = "Payment captured and wallet credited.", receiptNo = result?.ReceiptNo });
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"Error capturing payment for booking {request.BookingId}: {ex}");
                return StatusCode(500, new { success = false, message = "An error occurred while confirming the transaction." });
            }
        }

        private async Task TriggerRazorpaySplitSettlementAsync(string paymentId, decimal totalAmount, decimal gurujiShare, decimal trustShare, string gurujiId)
        {
            try
            {
                string keyId = _configuration["Razorpay:KeyId"] ?? "";
                string keySecret = _configuration["Razorpay:KeySecret"] ?? "";
                if (string.IsNullOrEmpty(keyId) || string.IsNullOrEmpty(keySecret) || keySecret == "rzp_test_placeholder_key_secret")
                {
                    // Gracefully skip call in trial/placeholder mode, log to local tracer
                    Console.WriteLine($"[Razorpay Split Route Tracer] Payment ID: {paymentId}, Total: {totalAmount}, GurujiShare: {gurujiShare}, TrustShare: {trustShare}");
                    return;
                }

                using var client = new HttpClient();
                var authHeader = Convert.ToBase64String(Encoding.ASCII.GetBytes($"{keyId}:{keySecret}"));
                client.DefaultRequestHeaders.Authorization = new System.Net.Http.Headers.AuthenticationHeaderValue("Basic", authHeader);

                var payload = new
                {
                    transfers = new[]
                    {
                        new
                        {
                            account = "acc_guruji_placeholder_id", // Resolved in production based on Guruji's linked bank account mapping
                            amount = (int)(gurujiShare * 100), // Razorpay accepts amounts in paise
                            currency = "INR",
                            notes = new { BookingPaymentId = paymentId }
                        }
                    }
                };

                var response = await client.PostAsync(
                    $"https://api.razorpay.com/v1/payments/{paymentId}/transfers",
                    new StringContent(System.Text.Json.JsonSerializer.Serialize(payload), Encoding.UTF8, "application/json")
                );

                Console.WriteLine($"Razorpay split transfer HTTP status: {response.StatusCode}");
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"Razorpay split routing failed: {ex.Message}");
            }
        }

        [HttpPut("{bookingId}/status")]
        [Authorize(Roles = "Admin")] // Requires Admin claim
        public async Task<IActionResult> UpdatePaymentStatus(string bookingId, [FromBody] UpdatePaymentStatusRequest request)
        {
            var adminId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(adminId)) return Unauthorized();

            if (request.PaymentStatus != "Paid")
            {
                using var connection = new SqlConnection(_connectionString);
                await connection.ExecuteAsync(
                    "UPDATE Bookings SET PaymentStatus = @Status, UpdatedAt = GETUTCDATE() WHERE Id = @Id",
                    new { Status = request.PaymentStatus, Id = bookingId }
                );
                return Ok(new { success = true });
            }

            // Direct Capture (No Maker-Checker flow checked!)
            using var conn = new SqlConnection(_connectionString);
            await conn.OpenAsync();

            try
            {
                var result = await conn.QueryFirstOrDefaultAsync<dynamic>(
                    "sp_Payment_Capture",
                    new
                    {
                        BookingId = bookingId,
                        PaymentId = "admin_captured_" + Guid.NewGuid().ToString().Substring(0, 8),
                        PaymentMethod = "admin_verified",
                        ActorId = adminId
                    },
                    commandType: CommandType.StoredProcedure
                );

                return Ok(new { success = true, message = "Payment marked Paid by Admin directly.", receiptNo = result?.ReceiptNo });
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"Error capturing payment directly: {ex}");
                return StatusCode(500, new { success = false, message = "An internal server error occurred while updating the transaction status." });
            }
        }

        [HttpPost("withdrawal")]
        public async Task<IActionResult> RequestWithdrawal([FromBody] WithdrawalRequest request)
        {
            var expertId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(expertId)) return Unauthorized();

            if (request.Amount <= 0)
            {
                return BadRequest(new { success = false, message = "Amount must be greater than zero." });
            }

            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            try
            {
                // 1. Submit the withdrawal entry in DB (deducts from balance and locks as Pending)
                await connection.ExecuteAsync(
                    "sp_Withdrawal_Create",
                    new
                    {
                        GurujiId = expertId,
                        Amount = request.Amount,
                        request.BankAccount,
                        request.IFSC,
                        request.UPI
                    },
                    commandType: CommandType.StoredProcedure
                );

                // Fetch the auto-incremented ID of the created withdrawal request
                var withdrawalId = await connection.QueryFirstOrDefaultAsync<int>(
                    "SELECT TOP 1 Id FROM WithdrawalRequests WHERE GurujiId = @GurujiId ORDER BY CreatedAt DESC",
                    new { GurujiId = expertId }
                );

                // 2. Trigger Instant Razorpay Payout to the Guruji's account / UPI
                var payoutResult = await TriggerRazorpayPayoutAsync(request.Amount, request.BankAccount, request.IFSC, request.UPI);

                if (payoutResult.Success)
                {
                    // Instantly Settle the payout in the database (updates WalletTransactions & logs ledger)
                    await connection.ExecuteAsync(
                        "sp_Withdrawal_UpdateStatus",
                        new
                        {
                            Id = withdrawalId,
                            Status = "Settled",
                            SettlementRef = payoutResult.PayoutId
                        },
                        commandType: CommandType.StoredProcedure
                    );

                    return Ok(new { success = true, message = "Instant payout settled successfully.", payoutId = payoutResult.PayoutId });
                }
                else
                {
                    // Fail the payout in the database, which releases the locked pending balance back to Guruji's wallet
                    await connection.ExecuteAsync(
                        "sp_Withdrawal_UpdateStatus",
                        new
                        {
                            Id = withdrawalId,
                            Status = "Failed",
                            SettlementRef = "Payout Failed: " + payoutResult.Error
                        },
                        commandType: CommandType.StoredProcedure
                    );

                    return BadRequest(new { success = false, message = "Instant payout failed. Wallet balance restored.", error = payoutResult.Error });
                }
            }
            catch (SqlException ex) when (ex.Message.Contains("INSUFFICIENT_WALLET_BALANCE"))
            {
                return BadRequest(new { success = false, message = "Insufficient wallet balance." });
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"Error requesting withdrawal: {ex}");
                return StatusCode(500, new { success = false, message = "An internal server error occurred while processing the withdrawal request." });
            }
        }

        [HttpGet("transactions")]
        public async Task<IActionResult> GetTransactions()
        {
            var gurujiId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(gurujiId)) return Unauthorized();

            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            try
            {
                var list = await connection.QueryAsync<dynamic>(
                    "sp_Guruji_GetTransactions",
                    new { GurujiId = gurujiId },
                    commandType: CommandType.StoredProcedure
                );

                return Ok(new { success = true, data = list });
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"Error fetching transactions for guruji {gurujiId}: {ex}");
                return StatusCode(500, new { success = false, message = "An error occurred while fetching transactions." });
            }
        }

        private async Task<(bool Success, string PayoutId, string? Error)> TriggerRazorpayPayoutAsync(decimal amount, string? bankAccount, string? ifsc, string? upi)
        {
            try
            {
                string keyId = _configuration["Razorpay:KeyId"] ?? "";
                string keySecret = _configuration["Razorpay:KeySecret"] ?? "";

                if (string.IsNullOrEmpty(keyId) || string.IsNullOrEmpty(keySecret) || keySecret == "rzp_test_placeholder_key_secret")
                {
                    // Placeholder test mode: instantly succeed with a mock payout ID
                    string mockPayoutId = "payout_" + Guid.NewGuid().ToString().Replace("-", "").Substring(0, 14);
                    Console.WriteLine($"[Razorpay Instant Payout Mock] Sent {amount} to Account: {bankAccount ?? "N/A"} / UPI: {upi ?? "N/A"}. Payout ID: {mockPayoutId}");
                    return (true, mockPayoutId, null);
                }

                using var client = new HttpClient();
                var authHeader = Convert.ToBase64String(Encoding.ASCII.GetBytes($"{keyId}:{keySecret}"));
                client.DefaultRequestHeaders.Authorization = new System.Net.Http.Headers.AuthenticationHeaderValue("Basic", authHeader);

                // Prepare Razorpay payout request payload
                var payload = new
                {
                    account_number = _configuration["Razorpay:FundAccountNumber"] ?? "409000021312", // Trust fund account
                    amount = (int)(amount * 100), // in paise
                    currency = "INR",
                    mode = string.IsNullOrEmpty(upi) ? "IMPS" : "UPI",
                    purpose = "payout",
                    fund_account = new
                    {
                        account_type = string.IsNullOrEmpty(upi) ? "bank_account" : "vpa",
                        bank_account = string.IsNullOrEmpty(upi) ? (object)new { name = "Guruji Partner", ifsc, account_number = bankAccount } : null,
                        vpa = !string.IsNullOrEmpty(upi) ? (object)new { address = upi } : null
                    },
                    queue_if_low_balance = true
                };

                var response = await client.PostAsync(
                    "https://api.razorpay.com/v1/payouts",
                    new StringContent(System.Text.Json.JsonSerializer.Serialize(payload), Encoding.UTF8, "application/json")
                );

                string responseBody = await response.Content.ReadAsStringAsync();
                if (response.IsSuccessStatusCode)
                {
                    using var doc = System.Text.Json.JsonDocument.Parse(responseBody);
                    string payoutId = doc.RootElement.GetProperty("id").GetString() ?? "payout_unknown";
                    return (true, payoutId, null);
                }
                else
                {
                    return (false, "", responseBody);
                }
            }
            catch (Exception ex)
            {
                return (false, "", ex.Message);
            }
        }

        [HttpPut("withdrawal/{id}/settle")]
        [Authorize(Roles = "Admin")]
        public async Task<IActionResult> SettleWithdrawal(int id, [FromBody] SettleWithdrawalRequest request)
        {
            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            try
            {
                await connection.ExecuteAsync(
                    "sp_Withdrawal_UpdateStatus",
                    new
                    {
                        Id = id,
                        Status = request.Status, // "Settled" or "Failed"
                        SettlementRef = request.SettlementRef
                    },
                    commandType: CommandType.StoredProcedure
                );

                 return Ok(new { success = true, message = $"Withdrawal marked as {request.Status} successfully." });
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"Error settling withdrawal {id}: {ex}");
                return StatusCode(500, new { success = false, message = "An internal server error occurred while settling the withdrawal." });
            }
        }

        [HttpGet("receipt/{bookingId}")]
        public async Task<IActionResult> GetReceipt(string bookingId)
        {
            using var connection = new SqlConnection(_connectionString);
            await connection.OpenAsync();

            try
            {
                var receipt = await connection.QueryFirstOrDefaultAsync<dynamic>(
                    "SELECT * FROM ReceiptSnapshots WHERE BookingId = @BookingId",
                    new { BookingId = bookingId }
                );

                if (receipt == null) return NotFound(new { success = false, message = "Receipt not found." });
                return Ok(new { success = true, data = receipt });
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"Error fetching receipt snapshot for booking {bookingId}: {ex}");
                return StatusCode(500, new { success = false, message = "An internal server error occurred while retrieving receipt snapshot." });
            }
        }

        private bool VerifyRazorpaySignature(string payload, string signature, string keySecret)
        {
            try
            {
                var keyBytes = System.Text.Encoding.UTF8.GetBytes(keySecret);
                var payloadBytes = System.Text.Encoding.UTF8.GetBytes(payload);
                using (var hmac = new System.Security.Cryptography.HMACSHA256(keyBytes))
                {
                    var hashBytes = hmac.ComputeHash(payloadBytes);
                    var calculatedSignature = BitConverter.ToString(hashBytes).Replace("-", "").ToLower();
                    return calculatedSignature == signature.ToLower();
                }
            }
            catch
            {
                return false;
            }
        }
    }

    public class CreateOrderRequest
    {
        public string BookingId { get; set; } = string.Empty;
    }

    public class VerifyPaymentRequest
    {
        public string BookingId { get; set; } = string.Empty;
        public string RazorpayPaymentId { get; set; } = string.Empty;
        public string RazorpaySignature { get; set; } = string.Empty;
    }

    public class UpdatePaymentStatusRequest
    {
        public string PaymentStatus { get; set; } = string.Empty;
    }

    public class WithdrawalRequest
    {
        public decimal Amount { get; set; }
        public string? BankAccount { get; set; }
        public string? IFSC { get; set; }
        public string? UPI { get; set; }
    }

    public class SettleWithdrawalRequest
    {
        public string Status { get; set; } = "Settled";
        public string? SettlementRef { get; set; }
    }
}
