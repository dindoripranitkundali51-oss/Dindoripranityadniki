-- =====================================================================
-- DINDORI PRANIT YADNYIKI - ENTERPRISE UPGRADES SCHEMA 06
-- Adds Double-Entry Ledger, Reliable Retrying Queues, and Idempotency
-- =====================================================================

-- 1. Create WalletTransactions Table
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'WalletTransactions')
BEGIN
    CREATE TABLE WalletTransactions (
        Id INT IDENTITY(1,1) PRIMARY KEY,
        GurujiId NVARCHAR(128) NOT NULL FOREIGN KEY REFERENCES Guruji(Uid),
        BookingId NVARCHAR(128) NULL FOREIGN KEY REFERENCES Bookings(Id),
        Amount DECIMAL(18,2) NOT NULL, -- Positive for credits, negative for debits
        TransactionType NVARCHAR(50) NOT NULL CHECK (TransactionType IN ('DakshinaShare', 'Withdrawal', 'Bonus', 'Refund')),
        ReferenceNo NVARCHAR(100) NULL,
        Description NVARCHAR(500) NULL,
        CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE()
    );
    CREATE INDEX IX_WalletTransactions_GurujiId ON WalletTransactions(GurujiId);
END
GO

-- 2. Create WebhookQueue Table
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'WebhookQueue')
BEGIN
    CREATE TABLE WebhookQueue (
        Id INT IDENTITY(1,1) PRIMARY KEY,
        Payload NVARCHAR(MAX) NOT NULL,
        TargetUrl NVARCHAR(500) NOT NULL,
        Status NVARCHAR(30) NOT NULL DEFAULT 'Pending' CHECK (Status IN ('Pending', 'Processing', 'Sent', 'Failed')),
        RetryCount INT NOT NULL DEFAULT 0,
        ErrorMessage NVARCHAR(MAX) NULL,
        NextAttemptAt DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
        CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE()
    );
END
GO

-- 3. Create SmsQueue Table
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'SmsQueue')
BEGIN
    CREATE TABLE SmsQueue (
        Id INT IDENTITY(1,1) PRIMARY KEY,
        MobileNumber NVARCHAR(20) NOT NULL,
        Message NVARCHAR(1000) NOT NULL,
        Status NVARCHAR(30) NOT NULL DEFAULT 'Pending' CHECK (Status IN ('Pending', 'Processing', 'Sent', 'Failed')),
        RetryCount INT NOT NULL DEFAULT 0,
        ErrorMessage NVARCHAR(MAX) NULL,
        NextAttemptAt DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
        CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE()
    );
END
GO

-- 4. Create FcmQueue Table
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'FcmQueue')
BEGIN
    CREATE TABLE FcmQueue (
        Id INT IDENTITY(1,1) PRIMARY KEY,
        FcmToken NVARCHAR(500) NOT NULL,
        Title NVARCHAR(250) NOT NULL,
        Body NVARCHAR(1000) NOT NULL,
        Status NVARCHAR(30) NOT NULL DEFAULT 'Pending' CHECK (Status IN ('Pending', 'Processing', 'Sent', 'Failed')),
        RetryCount INT NOT NULL DEFAULT 0,
        ErrorMessage NVARCHAR(MAX) NULL,
        NextAttemptAt DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
        CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE()
    );
END
GO

-- 5. Create IdempotentRequests Table
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'IdempotentRequests')
BEGIN
    CREATE TABLE IdempotentRequests (
        IdKey NVARCHAR(255) PRIMARY KEY,
        ResponseBody NVARCHAR(MAX) NOT NULL,
        ResponseStatusCode INT NOT NULL,
        CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE()
    );
    CREATE INDEX IX_IdempotentRequests_CreatedAt ON IdempotentRequests(CreatedAt);
END
GO

-- 6. Re-create sp_Payment_Capture to log double-entry split payments to WalletTransactions
IF EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID('sp_Payment_Capture') AND type = 'P')
    DROP PROCEDURE sp_Payment_Capture;
GO
CREATE PROCEDURE sp_Payment_Capture
    @BookingId NVARCHAR(128),
    @PaymentId NVARCHAR(100),
    @PaymentMethod NVARCHAR(50),
    @ActorId NVARCHAR(128)
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRANSACTION;
    BEGIN TRY
        DECLARE @GurujiId NVARCHAR(128);
        DECLARE @Amount DECIMAL(18,2);
        DECLARE @GurujiShare DECIMAL(18,2);
        DECLARE @TrustShare DECIMAL(18,2);
        DECLARE @DisplayId NVARCHAR(50);

        SELECT 
            @GurujiId = GurujiId,
            @Amount = Amount,
            @GurujiShare = GurujiShare,
            @TrustShare = TrustShare,
            @DisplayId = DisplayId
        FROM Bookings WHERE Id = @BookingId;

        -- Update Booking payment status
        UPDATE Bookings
        SET PaymentStatus = 'Paid',
            Status = 'Completed',
            RazorpayPaymentId = @PaymentId,
            UpdatedAt = GETUTCDATE()
        WHERE Id = @BookingId;

        -- Add record to financial ledger
        INSERT INTO FinancialLedger (BookingId, GurujiId, Amount, GurujiShare, TrustShare, Type, Status)
        VALUES (@BookingId, @GurujiId, @Amount, @GurujiShare, @TrustShare, 'Payment', 'Success');

        -- Add record to Double-Entry Wallet Transactions ledger
        INSERT INTO WalletTransactions (GurujiId, BookingId, Amount, TransactionType, ReferenceNo, Description)
        VALUES (@GurujiId, @BookingId, @GurujiShare, 'DakshinaShare', @PaymentId, 'Dakshina share from Pooja Seva payment');

        -- Add receipt snapshot record
        DECLARE @ReceiptNo NVARCHAR(100);
        DECLARE @Suffix NVARCHAR(8) = UPPER(RIGHT(REPLACE(@DisplayId, '-', ''), 8));
        SET @ReceiptNo = 'DPY-RCP-' + @Suffix + '-' + RIGHT(CAST(DATEDIFF(SECOND, '1970-01-01', GETUTCDATE()) AS VARCHAR(10)), 6);

        INSERT INTO ReceiptSnapshots (BookingId, ReceiptNo, Amount, PaymentStatus)
        VALUES (@BookingId, @ReceiptNo, @Amount, 'Paid');

        -- Release booking lock
        DELETE FROM BookingAssignmentLocks WHERE BookingId = @BookingId;

        -- Credit Guruji's wallet balance
        UPDATE Guruji
        SET WalletBalance = WalletBalance + @GurujiShare,
            TotalEarnings = TotalEarnings + @GurujiShare,
            UpdatedAt = GETUTCDATE()
        WHERE Uid = @GurujiId;

        -- Add event log
        INSERT INTO BookingEvents (BookingId, Type, Status, ActorId)
        VALUES (@BookingId, 'PAYMENT_VERIFIED', 'Completed', @ActorId);

        COMMIT TRANSACTION;
        SELECT @ReceiptNo AS ReceiptNo;
    END TRY
    BEGIN CATCH
        ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END;
GO

-- 7. Re-create sp_Withdrawal_UpdateStatus to log double-entry debit entries to WalletTransactions
IF EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID('sp_Withdrawal_UpdateStatus') AND type = 'P')
    DROP PROCEDURE sp_Withdrawal_UpdateStatus;
GO
CREATE PROCEDURE sp_Withdrawal_UpdateStatus
    @Id INT,
    @Status NVARCHAR(30),
    @SettlementRef NVARCHAR(100) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRANSACTION;
    BEGIN TRY
        DECLARE @GurujiId NVARCHAR(128);
        DECLARE @Amount DECIMAL(18,2);
        DECLARE @CurrentStatus NVARCHAR(30);

        SELECT @GurujiId = GurujiId, @Amount = Amount, @CurrentStatus = Status 
        FROM WithdrawalRequests WHERE Id = @Id;

        IF @CurrentStatus <> 'Pending' AND @CurrentStatus <> 'Processing'
        BEGIN
            THROW 50003, 'WITHDRAWAL_ALREADY_PROCESSED', 1;
        END

        -- Update request status
        UPDATE WithdrawalRequests
        SET Status = @Status,
            SettlementRef = @SettlementRef
        WHERE Id = @Id;

        -- If Settled (Success), deduct from balance and clear pending buffer
        IF @Status = 'Settled'
        BEGIN
            UPDATE Guruji
            SET WalletBalance = WalletBalance - @Amount,
                PendingWithdrawal = PendingWithdrawal - @Amount,
                UpdatedAt = GETUTCDATE()
            WHERE Uid = @GurujiId;

            -- Ledger Debit Entry (FinancialLedger)
            INSERT INTO FinancialLedger (BookingId, GurujiId, Amount, GurujiShare, TrustShare, Type, Status)
            VALUES ('WITHDRAWAL_' + CAST(@Id AS NVARCHAR(10)), @GurujiId, @Amount, @Amount, 0.00, 'Withdrawal', 'Success');

            -- Wallet Double-Entry Debit
            INSERT INTO WalletTransactions (GurujiId, BookingId, Amount, TransactionType, ReferenceNo, Description)
            VALUES (@GurujiId, NULL, -@Amount, 'Withdrawal', @SettlementRef, 'Wallet balance payout withdrawal');
        END
        -- If Failed / Rejected, release pending buffer back to Wallet balance
        ELSE IF @Status = 'Failed'
        BEGIN
            UPDATE Guruji
            SET PendingWithdrawal = PendingWithdrawal - @Amount,
                UpdatedAt = GETUTCDATE()
            WHERE Uid = @GurujiId;
        END

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END;
GO
