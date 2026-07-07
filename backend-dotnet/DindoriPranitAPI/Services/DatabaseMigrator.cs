using System;
using System.Data;
using System.IO;
using System.Text.RegularExpressions;
using Microsoft.Data.SqlClient;
using Dapper;

namespace DindoriPranitAPI.Services
{
    public static class DatabaseMigrator
    {
        public static void Migrate(string connectionString)
        {
            try
            {
                using var connection = new SqlConnection(connectionString);
                connection.Open();

                // Check if our enterprise updates are already applied
                bool alreadyMigrated = false;
                try
                {
                    // Check if WebhookSettings table exists
                    var result = connection.ExecuteScalar<int>(
                        "SELECT COUNT(*) FROM sys.tables WHERE name = 'WebhookSettings'"
                    );
                    alreadyMigrated = result > 0;
                }
                catch
                {
                    alreadyMigrated = false;
                }

                if (alreadyMigrated)
                {
                    Console.WriteLine("Database is already up to date. Skipping migrations.");
                    return;
                }

                Console.WriteLine("Applying Database Enterprise Migrations...");

                // Embed the SQL commands directly to avoid file IO path issues in production hosting
                string migrationSql = @"
-- 1. Alter Guruji Table to add KYC fields
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Guruji') AND name = 'PanCardUrl')
BEGIN
    ALTER TABLE Guruji ADD 
        PanCardUrl NVARCHAR(500) NULL,
        AadharCardUrl NVARCHAR(500) NULL,
        KycStatus NVARCHAR(30) NOT NULL DEFAULT 'Pending' CHECK (KycStatus IN ('Pending', 'Submitted', 'Approved', 'Rejected')),
        KycVerifiedAt DATETIME2 NULL;
END

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Guruji') AND name = 'PanNumber')
BEGIN
    ALTER TABLE Guruji ADD 
        PanNumber NVARCHAR(20) NULL,
        AadharNumber NVARCHAR(20) NULL;
END

-- 2. Alter AuditLogs Table to add advanced logging fields
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('AuditLogs') AND name = 'ClientIp')
BEGIN
    ALTER TABLE AuditLogs ADD 
        ClientIp NVARCHAR(50) NULL,
        RequestPayload NVARCHAR(MAX) NULL,
        ThreatLevel NVARCHAR(20) NOT NULL DEFAULT 'Low' CHECK (ThreatLevel IN ('Low', 'Medium', 'High', 'Critical'));
END

-- 3. Create ChatMessages Table
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'ChatMessages')
BEGIN
    CREATE TABLE ChatMessages (
        Id INT IDENTITY(1,1) PRIMARY KEY,
        SenderId NVARCHAR(128) NOT NULL,
        SenderName NVARCHAR(255) NOT NULL,
        ReceiverId NVARCHAR(128) NOT NULL,
        Message NVARCHAR(MAX) NOT NULL,
        IsRead BIT NOT NULL DEFAULT 0,
        CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE()
    );
    CREATE INDEX IX_ChatMessages_SenderId ON ChatMessages(SenderId);
    CREATE INDEX IX_ChatMessages_ReceiverId ON ChatMessages(ReceiverId);
END

-- 4. Create WebhookSettings Table
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'WebhookSettings')
BEGIN
    CREATE TABLE WebhookSettings (
        Id INT IDENTITY(1,1) PRIMARY KEY,
        Url NVARCHAR(500) NOT NULL UNIQUE,
        Secret NVARCHAR(255) NULL,
        IsActive BIT NOT NULL DEFAULT 1,
        CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE()
    );
END

-- 5. Stored Procedure: Update Guruji KYC
IF EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID('sp_Guruji_UpdateKyc') AND type = 'P')
    DROP PROCEDURE sp_Guruji_UpdateKyc;
";
                string migrationSqlPart2 = @"
CREATE PROCEDURE sp_Guruji_UpdateKyc
    @Uid NVARCHAR(128),
    @PanCardUrl NVARCHAR(500),
    @AadharCardUrl NVARCHAR(500),
    @KycStatus NVARCHAR(30)
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE Guruji
    SET PanCardUrl = @PanCardUrl,
        AadharCardUrl = @AadharCardUrl,
        KycStatus = @KycStatus,
        KycVerifiedAt = CASE WHEN @KycStatus = 'Approved' THEN GETUTCDATE() ELSE KycVerifiedAt END,
        UpdatedAt = GETUTCDATE()
    WHERE Uid = @Uid;
END;
";

                string migrationSqlPart3 = @"
-- 6. Stored Procedure: Insert Chat Message
IF EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID('sp_ChatMessage_Insert') AND type = 'P')
    DROP PROCEDURE sp_ChatMessage_Insert;
";
                string migrationSqlPart4 = @"
CREATE PROCEDURE sp_ChatMessage_Insert
    @SenderId NVARCHAR(128),
    @SenderName NVARCHAR(255),
    @ReceiverId NVARCHAR(128),
    @Message NVARCHAR(MAX)
AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO ChatMessages (SenderId, SenderName, ReceiverId, Message, IsRead, CreatedAt)
    VALUES (@SenderId, @SenderName, @ReceiverId, @Message, 0, GETUTCDATE());
END;
";

                string migrationSqlPart5 = @"
-- 7. Stored Procedure: Get Chat History
IF EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID('sp_ChatMessage_GetHistory') AND type = 'P')
    DROP PROCEDURE sp_ChatMessage_GetHistory;
";
                string migrationSqlPart6 = @"
CREATE PROCEDURE sp_ChatMessage_GetHistory
    @UserId1 NVARCHAR(128),
    @UserId2 NVARCHAR(128)
AS
BEGIN
    SET NOCOUNT ON;
    SELECT Id, SenderId, SenderName, ReceiverId, Message, IsRead, CreatedAt
    FROM ChatMessages
    WHERE (SenderId = @UserId1 AND ReceiverId = @UserId2)
       OR (SenderId = @UserId2 AND ReceiverId = @UserId1)
    ORDER BY CreatedAt ASC;
END;
";

                string migrationSqlPart7 = @"
-- 8. Stored Procedure: Save Webhook Setting
IF EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID('sp_WebhookSettings_Save') AND type = 'P')
    DROP PROCEDURE sp_WebhookSettings_Save;
";
                string migrationSqlPart8 = @"
CREATE PROCEDURE sp_WebhookSettings_Save
    @Url NVARCHAR(500),
    @Secret NVARCHAR(255),
    @IsActive BIT
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM WebhookSettings WHERE Url = @Url)
    BEGIN
        UPDATE WebhookSettings
        SET Secret = @Secret,
            IsActive = @IsActive
        WHERE Url = @Url;
    END
    ELSE
    BEGIN
        INSERT INTO WebhookSettings (Url, Secret, IsActive, CreatedAt)
        VALUES (@Url, @Secret, @IsActive, GETUTCDATE());
    END
END;
";

                string migrationSqlPart9 = @"
-- 9. Stored Procedure: Advanced Audit Log Insert
IF EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID('sp_AuditLog_Insert_Advanced') AND type = 'P')
    DROP PROCEDURE sp_AuditLog_Insert_Advanced;
";
                string migrationSqlPart10 = @"
CREATE PROCEDURE sp_AuditLog_Insert_Advanced
    @AdminId NVARCHAR(128),
    @Action NVARCHAR(100),
    @TargetId NVARCHAR(128),
    @Details NVARCHAR(1000),
    @ClientIp NVARCHAR(50),
    @RequestPayload NVARCHAR(MAX),
    @ThreatLevel NVARCHAR(20)
AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO AuditLogs (AdminId, Action, TargetId, Details, ClientIp, RequestPayload, ThreatLevel, CreatedAt)
    VALUES (@AdminId, @Action, @TargetId, @Details, @ClientIp, @RequestPayload, @ThreatLevel, GETUTCDATE());
END;
";

                string migrationSqlPart11 = @"
-- 1. Create WalletTransactions Table
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'WalletTransactions')
BEGIN
    CREATE TABLE WalletTransactions (
        Id INT IDENTITY(1,1) PRIMARY KEY,
        GurujiId NVARCHAR(128) NOT NULL FOREIGN KEY REFERENCES Guruji(Uid),
        BookingId NVARCHAR(128) NULL FOREIGN KEY REFERENCES Bookings(Id),
        Amount DECIMAL(18,2) NOT NULL,
        TransactionType NVARCHAR(50) NOT NULL CHECK (TransactionType IN ('DakshinaShare', 'Withdrawal', 'Bonus', 'Refund')),
        ReferenceNo NVARCHAR(100) NULL,
        Description NVARCHAR(500) NULL,
        CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE()
    );
END
";
                string migrationSqlPart12 = @"
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
";
                string migrationSqlPart13 = @"
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
";
                string migrationSqlPart14 = @"
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
";
                string migrationSqlPart15 = @"
-- 5. Create IdempotentRequests Table
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'IdempotentRequests')
BEGIN
    CREATE TABLE IdempotentRequests (
        IdKey NVARCHAR(255) PRIMARY KEY,
        ResponseBody NVARCHAR(MAX) NOT NULL,
        ResponseStatusCode INT NOT NULL,
        CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE()
    );
END
";
                string migrationSqlPart16 = @"
-- 6. Re-create sp_Payment_Capture to log double-entry split payments to WalletTransactions
IF EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID('sp_Payment_Capture') AND type = 'P')
    DROP PROCEDURE sp_Payment_Capture;
";
                string migrationSqlPart17 = @"
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

        UPDATE Bookings
        SET PaymentStatus = 'Paid',
            Status = 'Completed',
            RazorpayPaymentId = @PaymentId,
            UpdatedAt = GETUTCDATE()
        WHERE Id = @BookingId;

        INSERT INTO FinancialLedger (BookingId, GurujiId, Amount, GurujiShare, TrustShare, Type, Status)
        VALUES (@BookingId, @GurujiId, @Amount, @GurujiShare, @TrustShare, 'Payment', 'Success');

        INSERT INTO WalletTransactions (GurujiId, BookingId, Amount, TransactionType, ReferenceNo, Description)
        VALUES (@GurujiId, @BookingId, @GurujiShare, 'DakshinaShare', @PaymentId, 'Dakshina share from Pooja Seva payment');

        DECLARE @ReceiptNo NVARCHAR(100);
        DECLARE @Suffix NVARCHAR(8) = UPPER(RIGHT(REPLACE(@DisplayId, '-', ''), 8));
        SET @ReceiptNo = 'DPY-RCP-' + @Suffix + '-' + RIGHT(CAST(DATEDIFF(SECOND, '1970-01-01', GETUTCDATE()) AS VARCHAR(10)), 6);

        INSERT INTO ReceiptSnapshots (BookingId, ReceiptNo, Amount, PaymentStatus)
        VALUES (@BookingId, @ReceiptNo, @Amount, 'Paid');

        DELETE FROM BookingAssignmentLocks WHERE BookingId = @BookingId;

        UPDATE Guruji
        SET WalletBalance = WalletBalance + @GurujiShare,
            TotalEarnings = TotalEarnings + @GurujiShare,
            UpdatedAt = GETUTCDATE()
        WHERE Uid = @GurujiId;

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
";
                string migrationSqlPart18 = @"
-- 7. Re-create sp_Withdrawal_UpdateStatus to log double-entry debit entries to WalletTransactions
IF EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID('sp_Withdrawal_UpdateStatus') AND type = 'P')
    DROP PROCEDURE sp_Withdrawal_UpdateStatus;
";
                string migrationSqlPart19 = @"
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

        UPDATE WithdrawalRequests
        SET Status = @Status,
            SettlementRef = @SettlementRef
        WHERE Id = @Id;

        IF @Status = 'Settled'
        BEGIN
            UPDATE Guruji
            SET WalletBalance = WalletBalance - @Amount,
                PendingWithdrawal = PendingWithdrawal - @Amount,
                UpdatedAt = GETUTCDATE()
            WHERE Uid = @GurujiId;

            INSERT INTO FinancialLedger (BookingId, GurujiId, Amount, GurujiShare, TrustShare, Type, Status)
            VALUES ('WITHDRAWAL_' + CAST(@Id AS NVARCHAR(10)), @GurujiId, @Amount, @Amount, 0.00, 'Withdrawal', 'Success');

            INSERT INTO WalletTransactions (GurujiId, BookingId, Amount, TransactionType, ReferenceNo, Description)
            VALUES (@GurujiId, NULL, -@Amount, 'Withdrawal', @SettlementRef, 'Wallet balance payout withdrawal');
        END
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
";

                string migrationSqlPart20 = @"
-- 1. Alter Guruji Table to add HomeLat and HomeLng
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Guruji') AND name = 'HomeLat')
BEGIN
    ALTER TABLE Guruji ADD 
        HomeLat FLOAT NOT NULL DEFAULT 0.0,
        HomeLng FLOAT NOT NULL DEFAULT 0.0;
END
";
                string migrationSqlPart21 = @"
UPDATE Guruji 
SET HomeLat = Lat, HomeLng = Lng 
WHERE HomeLat = 0.0 AND Lat <> 0.0;
";
                string migrationSqlPart22 = @"
-- 2. Drop inline Status check constraint and replace with CK_Bookings_Status
DECLARE @ConstraintName NVARCHAR(255);
SELECT @ConstraintName = dc.name
FROM sys.check_constraints dc
INNER JOIN sys.columns c ON dc.parent_column_id = c.column_id AND dc.parent_object_id = c.object_id
WHERE dc.parent_object_id = OBJECT_ID('Bookings') AND c.name = 'Status';

IF @ConstraintName IS NOT NULL
BEGIN
    EXEC('ALTER TABLE Bookings DROP CONSTRAINT ' + @ConstraintName);
END

IF NOT EXISTS (SELECT * FROM sys.objects WHERE parent_object_id = OBJECT_ID('Bookings') AND name = 'CK_Bookings_Status' AND type = 'C')
BEGIN
    ALTER TABLE Bookings ADD CONSTRAINT CK_Bookings_Status 
    CHECK (Status IN ('Pending', 'Assigned', 'Accepted', 'Departed', 'Arrived', 'InProgress', 'PaymentPending', 'Completed', 'Cancelled'));
END
";

                string migrationSqlPart23 = @"
-- 3. Create EmailQueue Table
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'EmailQueue')
BEGIN
    CREATE TABLE EmailQueue (
        Id INT IDENTITY(1,1) PRIMARY KEY,
        ToEmail NVARCHAR(255) NOT NULL,
        Subject NVARCHAR(255) NOT NULL,
        Body NVARCHAR(MAX) NOT NULL,
        Status NVARCHAR(30) NOT NULL DEFAULT 'Pending' CHECK (Status IN ('Pending', 'Processing', 'Sent', 'Failed')),
        RetryCount INT NOT NULL DEFAULT 0,
        ErrorMessage NVARCHAR(MAX) NULL,
        NextAttemptAt DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
        CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE()
    );
END
";

                // Execute SQL parts sequentially
                connection.Execute(migrationSql);
                connection.Execute(migrationSqlPart2);
                connection.Execute(migrationSqlPart3);
                connection.Execute(migrationSqlPart4);
                connection.Execute(migrationSqlPart5);
                connection.Execute(migrationSqlPart6);
                connection.Execute(migrationSqlPart7);
                connection.Execute(migrationSqlPart8);
                connection.Execute(migrationSqlPart9);
                connection.Execute(migrationSqlPart10);
                connection.Execute(migrationSqlPart11);
                connection.Execute(migrationSqlPart12);
                connection.Execute(migrationSqlPart13);
                connection.Execute(migrationSqlPart14);
                connection.Execute(migrationSqlPart15);
                connection.Execute(migrationSqlPart16);
                connection.Execute(migrationSqlPart17);
                connection.Execute(migrationSqlPart18);
                connection.Execute(migrationSqlPart19);
                connection.Execute(migrationSqlPart20);
                connection.Execute(migrationSqlPart21);
                connection.Execute(migrationSqlPart22);
                connection.Execute(migrationSqlPart23);

                string migrationSqlPart24 = @"
-- 1. Create GurujiAvailability Table
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'GurujiAvailability')
BEGIN
    CREATE TABLE GurujiAvailability (
        Id INT IDENTITY(1,1) PRIMARY KEY,
        GurujiId NVARCHAR(128) NOT NULL FOREIGN KEY REFERENCES Guruji(Uid),
        AvailableDate DATE NOT NULL,
        CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
        CONSTRAINT UC_Guruji_AvailableDate UNIQUE(GurujiId, AvailableDate)
    );
    CREATE INDEX IX_GurujiAvailability_Date ON GurujiAvailability(AvailableDate);
END
";
                string migrationSqlPart25 = @"
-- 2. Drop and Re-create sp_GurujiAvailability_Save
IF EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID('sp_GurujiAvailability_Save') AND type = 'P')
    DROP PROCEDURE sp_GurujiAvailability_Save;
";
                string migrationSqlPart26 = @"
CREATE PROCEDURE sp_GurujiAvailability_Save
    @GurujiId NVARCHAR(128),
    @DateList NVARCHAR(MAX)
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRANSACTION;
    BEGIN TRY
        DELETE FROM GurujiAvailability 
        WHERE GurujiId = @GurujiId 
          AND AvailableDate >= CAST(GETUTCDATE() AS DATE);

        IF @DateList IS NOT NULL AND LTRIM(RTRIM(@DateList)) <> ''
        BEGIN
            INSERT INTO GurujiAvailability (GurujiId, AvailableDate)
            SELECT DISTINCT @GurujiId, CAST(value AS DATE)
            FROM STRING_SPLIT(@DateList, ',')
            WHERE LTRIM(RTRIM(value)) <> ''
              AND CAST(value AS DATE) >= CAST(GETUTCDATE() AS DATE);
        END

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END;
";
                string migrationSqlPart27 = @"
-- 3. Drop and Re-create sp_GurujiAvailability_Get
IF EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID('sp_GurujiAvailability_Get') AND type = 'P')
    DROP PROCEDURE sp_GurujiAvailability_Get;
";
                string migrationSqlPart28 = @"
CREATE PROCEDURE sp_GurujiAvailability_Get
    @GurujiId NVARCHAR(128)
AS
BEGIN
    SET NOCOUNT ON;
    SELECT CONVERT(NVARCHAR(10), AvailableDate, 23) AS AvailableDate
    FROM GurujiAvailability
    WHERE GurujiId = @GurujiId 
      AND AvailableDate >= CAST(GETUTCDATE() AS DATE)
    ORDER BY AvailableDate ASC;
END;
";
                string migrationSqlPart29 = @"
-- 4. Drop and Re-create sp_Booking_GetAvailableDates
IF EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID('sp_Booking_GetAvailableDates') AND type = 'P')
    DROP PROCEDURE sp_Booking_GetAvailableDates;
";
                string migrationSqlPart30 = @"
CREATE PROCEDURE sp_Booking_GetAvailableDates
AS
BEGIN
    SET NOCOUNT ON;
    SELECT DISTINCT CONVERT(NVARCHAR(10), a.AvailableDate, 23) AS AvailableDate
    FROM GurujiAvailability a
    INNER JOIN Guruji g ON a.GurujiId = g.Uid
    WHERE g.Status IN ('Approved', 'Active')
      AND g.IsAvailable = 1
      AND a.AvailableDate >= CAST(GETUTCDATE() AS DATE)
    ORDER BY AvailableDate ASC;
END;
";

                string migrationSqlPart31 = @"
-- 5. Drop and Re-create sp_Guruji_GetTransactions
IF EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID('sp_Guruji_GetTransactions') AND type = 'P')
    DROP PROCEDURE sp_Guruji_GetTransactions;
";
                string migrationSqlPart32 = @"
CREATE PROCEDURE sp_Guruji_GetTransactions
    @GurujiId NVARCHAR(128)
AS
BEGIN
    SET NOCOUNT ON;
    SELECT 
        Id,
        BookingId,
        Amount,
        TransactionType,
        ReferenceNo,
        Description,
        CONVERT(NVARCHAR(30), CreatedAt, 126) AS CreatedAt
    FROM WalletTransactions
    WHERE GurujiId = @GurujiId
    ORDER BY CreatedAt DESC;
END;
";

                connection.Execute(migrationSqlPart24);
                connection.Execute(migrationSqlPart25);
                connection.Execute(migrationSqlPart26);
                connection.Execute(migrationSqlPart27);
                connection.Execute(migrationSqlPart28);
                connection.Execute(migrationSqlPart29);
                connection.Execute(migrationSqlPart30);
                connection.Execute(migrationSqlPart31);
                connection.Execute(migrationSqlPart32);

                Console.WriteLine("Database Enterprise Migrations applied successfully.");
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"Database Migration Error: {ex.Message}");
                // Let application proceed, or crash depending on requirements.
            }
        }
    }
}
