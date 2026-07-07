-- =====================================================================
-- DINDORI PRANIT YADNYIKI & VASTU SEVA
-- STORED PROCEDURES (MS SQL SERVER)
-- =====================================================================

-- ==========================================
-- १. USERS (यजमान) PROCEDURES
-- ==========================================

-- Get user by Mobile (लॉगिनसाठी)
CREATE PROCEDURE sp_User_GetByMobile
    @Mobile NVARCHAR(20)
AS
BEGIN
    SET NOCOUNT ON;
    SELECT * FROM Users WHERE Mobile = @Mobile;
END;
GO

-- Get user by ID
CREATE PROCEDURE sp_User_GetById
    @Uid NVARCHAR(128)
AS
BEGIN
    SET NOCOUNT ON;
    SELECT * FROM Users WHERE Uid = @Uid;
END;
GO

-- Register/Insert new user
CREATE PROCEDURE sp_User_Insert
    @Uid NVARCHAR(128),
    @FullName NVARCHAR(255),
    @Mobile NVARCHAR(20),
    @Email NVARCHAR(255) = NULL,
    @Address NVARCHAR(500) = NULL,
    @District NVARCHAR(100) = NULL,
    @Pincode NVARCHAR(10) = NULL,
    @Lat FLOAT = 0.0,
    @Lng FLOAT = 0.0
AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO Users (Uid, FullName, Mobile, Email, Address, District, Pincode, Lat, Lng)
    VALUES (@Uid, @FullName, @Mobile, @Email, @Address, @District, @Pincode, @Lat, @Lng);
END;
GO

-- Update User FCM Token
CREATE PROCEDURE sp_User_UpdateFcmToken
    @Uid NVARCHAR(128),
    @FcmToken NVARCHAR(500)
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE Users 
    SET FcmToken = @FcmToken, UpdatedAt = GETUTCDATE()
    WHERE Uid = @Uid;
END;
GO

-- Update User Status
CREATE PROCEDURE sp_User_UpdateStatus
    @Uid NVARCHAR(128),
    @Status NVARCHAR(20)
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE Users
    SET Status = @Status, UpdatedAt = GETUTCDATE()
    WHERE Uid = @Uid;
END;
GO

-- ==========================================
-- २. GURUJI & VASTU EXPERT PROCEDURES
-- ==========================================

-- Get Guruji/Expert by Mobile
CREATE PROCEDURE sp_Guruji_GetByMobile
    @Mobile NVARCHAR(20)
AS
BEGIN
    SET NOCOUNT ON;
    SELECT * FROM Guruji WHERE Mobile = @Mobile;
END;
GO

-- Get Guruji/Expert by ID
CREATE PROCEDURE sp_Guruji_GetById
    @Uid NVARCHAR(128)
AS
BEGIN
    SET NOCOUNT ON;
    SELECT * FROM Guruji WHERE Uid = @Uid;
END;
GO

-- Register new Guruji/Expert
CREATE PROCEDURE sp_Guruji_Insert
    @Uid NVARCHAR(128),
    @FullName NVARCHAR(255),
    @Mobile NVARCHAR(20),
    @Email NVARCHAR(255) = NULL,
    @Address NVARCHAR(500) = NULL,
    @District NVARCHAR(100) = NULL,
    @Pincode NVARCHAR(10) = NULL,
    @Lat FLOAT = 0.0,
    @Lng FLOAT = 0.0,
    @Expertises NVARCHAR(MAX),
    @ExpertType NVARCHAR(20)
AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO Guruji (Uid, FullName, Mobile, Email, Address, District, Pincode, Lat, Lng, Expertises, ExpertType, Status)
    VALUES (@Uid, @FullName, @Mobile, @Email, @Address, @District, @Pincode, @Lat, @Lng, @Expertises, @ExpertType, 'Pending');
END;
GO

-- Update Availability status
CREATE PROCEDURE sp_Guruji_UpdateAvailability
    @Uid NVARCHAR(128),
    @IsAvailable BIT
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE Guruji
    SET IsAvailable = @IsAvailable, UpdatedAt = GETUTCDATE()
    WHERE Uid = @Uid;
END;
GO

-- Update Guruji Wallet Balance
CREATE PROCEDURE sp_Guruji_UpdateWallet
    @Uid NVARCHAR(128),
    @Amount DECIMAL(18,2),
    @IsCredit BIT -- 1 for Add, 0 for Subtract
AS
BEGIN
    SET NOCOUNT ON;
    IF @IsCredit = 1
    BEGIN
        UPDATE Guruji
        SET WalletBalance = WalletBalance + @Amount,
            TotalEarnings = TotalEarnings + @Amount,
            UpdatedAt = GETUTCDATE()
        WHERE Uid = @Uid;
    END
    ELSE
    BEGIN
        UPDATE Guruji
        SET WalletBalance = WalletBalance - @Amount,
            UpdatedAt = GETUTCDATE()
        WHERE Uid = @Uid;
    END
END;
GO

-- Update Guruji Status (Approve/Reject)
CREATE PROCEDURE sp_Guruji_UpdateStatus
    @Uid NVARCHAR(128),
    @Status NVARCHAR(20)
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE Guruji
    SET Status = @Status,
        IsAvailable = CASE WHEN @Status IN ('Approved', 'Active') THEN 1 ELSE 0 END,
        UpdatedAt = GETUTCDATE()
    WHERE Uid = @Uid;
END;
GO

-- Available Expert Matching Engine (यज्ञिकी साठी गुरुजी आणि वास्तू सेवेसाठी वास्तू तज्ञ शोधणे)
CREATE PROCEDURE sp_Guruji_GetAvailableForDate
    @Date DATE,
    @RequiredExpertise NVARCHAR(100),
    @RequiredExpertType NVARCHAR(20),
    @UserLat FLOAT,
    @UserLng FLOAT
AS
BEGIN
    SET NOCOUNT ON;

    -- Haversine formula calculation inside query to filter by proximity and sort by score
    SELECT 
        Uid, FullName, Mobile, Lat, Lng, IsAvailable, ExpertType,
        ( 3959 * acos( cos( radians(@UserLat) ) * cos( radians( Lat ) ) * cos( radians( Lng ) - radians(@UserLng) ) + sin( radians(@UserLat) ) * sin( radians( Lat ) ) ) ) AS DistanceInMiles
    FROM Guruji
    WHERE 
        Status IN ('Approved', 'Active')
        AND IsAvailable = 1
        AND ExpertType = @RequiredExpertType
        AND (Expertises LIKE '%' + @RequiredExpertise + '%' OR @RequiredExpertise IS NULL)
        AND Uid IN (
            -- Filter only gurujis who marked themselves available on this date
            SELECT GurujiId FROM GurujiAvailability WHERE AvailableDate = @Date
        )
        AND Uid NOT IN (
            -- Exclude guruji if already locked or has a booking on this date
            SELECT GurujiId FROM BookingAssignmentLocks WHERE LockDate = @Date
        )
    ORDER BY DistanceInMiles ASC;
END;
GO

-- ==========================================
-- ३. BOOKINGS (बुकिंग) PROCEDURES
-- ==========================================

-- Create Booking Idempotency Check
CREATE PROCEDURE sp_BookingIdempotency_Check
    @UserId NVARCHAR(128),
    @ClientRequestId NVARCHAR(128),
    @IsExists BIT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM BookingIdempotency WHERE UserId = @UserId AND ClientRequestId = @ClientRequestId)
        SET @IsExists = 1;
    ELSE
        SET @IsExists = 0;
END;
GO

-- Save Idempotency Request Key
CREATE PROCEDURE sp_BookingIdempotency_Save
    @UserId NVARCHAR(128),
    @ClientRequestId NVARCHAR(128)
AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO BookingIdempotency (UserId, ClientRequestId) VALUES (@UserId, @ClientRequestId);
END;
GO

-- Auto-Increment Booking Sequence Display ID Generation
CREATE PROCEDURE sp_Booking_GetNextDisplayId
    @NextId NVARCHAR(50) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @Count INT;
    SELECT @Count = COUNT(*) FROM Bookings;
    SET @NextId = 'DPY-' + RIGHT('00000' + CAST((@Count + 1) AS VARCHAR(10)), 6);
END;
GO

-- Create new Booking record
CREATE PROCEDURE sp_Booking_Create
    @Id NVARCHAR(128),
    @UserId NVARCHAR(128),
    @PoojaId NVARCHAR(50),
    @Date DATE,
    @Amount DECIMAL(18,2),
    @ContactName NVARCHAR(255),
    @ContactPhone NVARCHAR(20),
    @Address NVARCHAR(500),
    @District NVARCHAR(100),
    @Pincode NVARCHAR(10),
    @UserLat FLOAT,
    @UserLng FLOAT
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @DisplayId NVARCHAR(50);
    EXEC sp_Booking_GetNextDisplayId @DisplayId OUTPUT;

    -- Default commissions (Yajman gets 30%, Trust gets 70% share)
    DECLARE @GurujiShare DECIMAL(18,2) = @Amount * 0.30;
    DECLARE @TrustShare DECIMAL(18,2) = @Amount * 0.70;

    INSERT INTO Bookings (Id, DisplayId, UserId, PoojaId, Date, Amount, GurujiShare, TrustShare, ContactName, ContactPhone, Address, District, Pincode, UserLat, UserLng, Status, PaymentStatus)
    VALUES (@Id, @DisplayId, @UserId, @PoojaId, @Date, @Amount, @GurujiShare, @TrustShare, @ContactName, @ContactPhone, @Address, @District, @Pincode, @UserLat, @UserLng, 'Pending', 'Pending');

    -- Insert Initial Booking Event Log
    INSERT INTO BookingEvents (BookingId, Type, Status, ActorId)
    VALUES (@Id, 'BOOKING_CREATED', 'Pending', @UserId);

    SELECT * FROM Bookings WHERE Id = @Id;
END;
GO

-- Get Booking by ID
CREATE PROCEDURE sp_Booking_GetById
    @Id NVARCHAR(128)
AS
BEGIN
    SET NOCOUNT ON;
    SELECT b.*, p.Name AS PoojaName, p.SevaType, g.FullName AS GurujiName, g.Lat AS GurujiLat, g.Lng AS GurujiLng
    FROM Bookings b
    JOIN Poojas p ON b.PoojaId = p.Id
    LEFT JOIN Guruji g ON b.GurujiId = g.Uid
    WHERE b.Id = @Id;
END;
GO

-- Get Bookings list for a User
CREATE PROCEDURE sp_Booking_GetByUserId
    @UserId NVARCHAR(128)
AS
BEGIN
    SET NOCOUNT ON;
    SELECT b.*, p.Name AS PoojaName 
    FROM Bookings b
    JOIN Poojas p ON b.PoojaId = p.Id
    WHERE b.UserId = @UserId
    ORDER BY b.CreatedAt DESC;
END;
GO

-- Get Assigned Bookings list for Guruji
CREATE PROCEDURE sp_Booking_GetByGurujiId
    @GurujiId NVARCHAR(128)
AS
BEGIN
    SET NOCOUNT ON;
    SELECT b.*, p.Name AS PoojaName 
    FROM Bookings b
    JOIN Poojas p ON b.PoojaId = p.Id
    WHERE b.GurujiId = @GurujiId
    ORDER BY b.Date ASC, b.CreatedAt DESC;
END;
GO

-- Assign Expert (Guruji/Vastu Expert) to Booking
CREATE PROCEDURE sp_Booking_AssignGuruji
    @BookingId NVARCHAR(128),
    @GurujiId NVARCHAR(128),
    @Date DATE
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRANSACTION;
    BEGIN TRY
        -- Check if lock already exists for this guruji on this date
        IF EXISTS (SELECT 1 FROM BookingAssignmentLocks WHERE LockDate = @Date AND GurujiId = @GurujiId)
        BEGIN
            THROW 50001, 'GURUJI_SLOT_LOCKED', 1;
        END

        -- Update booking assignment
        UPDATE Bookings
        SET GurujiId = @GurujiId,
            Status = 'Assigned',
            UpdatedAt = GETUTCDATE()
        WHERE Id = @BookingId;

        -- Create date-level assignment lock
        INSERT INTO BookingAssignmentLocks (LockDate, GurujiId, BookingId)
        VALUES (@Date, @GurujiId, @BookingId);

        -- Add Event Log
        INSERT INTO BookingEvents (BookingId, Type, Status, ActorId)
        VALUES (@BookingId, 'GURUJI_ASSIGNED', 'Assigned', 'System');

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END;
GO

-- Lock Release (Rejection / Cancellation)
CREATE PROCEDURE sp_BookingLock_Release
    @Date DATE,
    @GurujiId NVARCHAR(128),
    @BookingId NVARCHAR(128)
AS
BEGIN
    SET NOCOUNT ON;
    DELETE FROM BookingAssignmentLocks 
    WHERE LockDate = @Date AND GurujiId = @GurujiId AND BookingId = @BookingId;
END;
GO

-- Update Booking Status
CREATE PROCEDURE sp_Booking_UpdateStatus
    @BookingId NVARCHAR(128),
    @Status NVARCHAR(30),
    @ActorId NVARCHAR(128)
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE Bookings
    SET Status = @Status,
        UpdatedAt = GETUTCDATE()
    WHERE Id = @BookingId;

    INSERT INTO BookingEvents (BookingId, Type, Status, ActorId)
    VALUES (@BookingId, 'STATUS_UPDATED', @Status, @ActorId);
END;
GO

-- ==========================================
-- ४. OTP MANAGEMENT PROCEDURES
-- ==========================================

-- Create OTP reference record
CREATE PROCEDURE sp_BookingOtp_Create
    @BookingId NVARCHAR(128),
    @OtpHash NVARCHAR(500),
    @OtpSalt NVARCHAR(500),
    @ExpiresAt DATETIME2
AS
BEGIN
    SET NOCOUNT ON;
    -- Merge record to overwrite existing requested OTPs
    MERGE BookingOtps AS Target
    USING (SELECT @BookingId AS BookingId) AS Source
    ON (Target.BookingId = Source.BookingId)
    WHEN MATCHED THEN
        UPDATE SET OtpHash = @OtpHash, OtpSalt = @OtpSalt, ExpiresAt = @ExpiresAt
    WHEN NOT MATCHED THEN
        INSERT (BookingId, OtpHash, OtpSalt, ExpiresAt)
        VALUES (@BookingId, @OtpHash, @OtpSalt, @ExpiresAt);
END;
GO

-- Verify OTP
CREATE PROCEDURE sp_BookingOtp_Get
    @BookingId NVARCHAR(128)
AS
BEGIN
    SET NOCOUNT ON;
    SELECT * FROM BookingOtps WHERE BookingId = @BookingId;
END;
GO

-- Delete OTP once verified
CREATE PROCEDURE sp_BookingOtp_Delete
    @BookingId NVARCHAR(128)
AS
BEGIN
    SET NOCOUNT ON;
    DELETE FROM BookingOtps WHERE BookingId = @BookingId;
END;
GO

-- ==========================================
-- ५. PAYMENTS (पेमेंट) PROCEDURES
-- ==========================================

-- Process Payment Verification and capture (Split commission, credit wallet)
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
        -- Fetch booking details
        DECLARE @GurujiId NVARCHAR(128);
        DECLARE @Amount DECIMAL(18,2);
        DECLARE @GurujiShare DECIMAL(18,2);
        DECLARE @TrustShare DECIMAL(18,2);
        DECLARE @DisplayId NVARCHAR(50);
        DECLARE @Date DATE;

        SELECT 
            @GurujiId = GurujiId,
            @Amount = Amount,
            @GurujiShare = GurujiShare,
            @TrustShare = TrustShare,
            @DisplayId = DisplayId,
            @Date = Date
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

        -- Add receipt snapshot record
        DECLARE @ReceiptNo NVARCHAR(100);
        DECLARE @Suffix NVARCHAR(8) = UPPER(RIGHT(REPLACE(@DisplayId, '-', ''), 8));
        SET @ReceiptNo = 'DPY-RCP-' + @Suffix + '-' + RIGHT(CAST(DATEDIFF(SECOND, '1970-01-01', GETUTCDATE()) AS VARCHAR(10)), 6);

        INSERT INTO ReceiptSnapshots (BookingId, ReceiptNo, Amount, PaymentStatus)
        VALUES (@BookingId, @ReceiptNo, @Amount, 'Paid');

        -- Release booking lock so guruji can take new assignments on this day
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

-- Verify receipt publicly
CREATE PROCEDURE sp_Receipt_GetByReceiptNo
    @ReceiptNo NVARCHAR(100)
AS
BEGIN
    SET NOCOUNT ON;
    SELECT r.*, b.ContactName, b.ContactPhone, p.Name AS PoojaName
    FROM ReceiptSnapshots r
    JOIN Bookings b ON r.BookingId = b.Id
    JOIN Poojas p ON b.PoojaId = p.Id
    WHERE r.ReceiptNo = @ReceiptNo;
END;
GO

-- Guruji Wallet Withdrawal Request
CREATE PROCEDURE sp_Withdrawal_Create
    @GurujiId NVARCHAR(128),
    @Amount DECIMAL(18,2),
    @BankAccount NVARCHAR(50) = NULL,
    @IFSC NVARCHAR(20) = NULL,
    @UPI NVARCHAR(100) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRANSACTION;
    BEGIN TRY
        -- Check if guruji has sufficient balance
        DECLARE @AvailableBalance DECIMAL(18,2);
        SELECT @AvailableBalance = (WalletBalance - PendingWithdrawal) FROM Guruji WHERE Uid = @GurujiId;

        IF @AvailableBalance < @Amount
        BEGIN
            THROW 50002, 'INSUFFICIENT_WALLET_BALANCE', 1;
        END

        -- Insert Withdrawal Record
        INSERT INTO WithdrawalRequests (GurujiId, Amount, BankAccount, IFSC, UPI, Status)
        VALUES (@GurujiId, @Amount, @BankAccount, @IFSC, @UPI, 'Pending');

        -- Update Guruji's pending withdrawal buffer
        UPDATE Guruji
        SET PendingWithdrawal = PendingWithdrawal + @Amount,
            UpdatedAt = GETUTCDATE()
        WHERE Uid = @GurujiId;

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END;
GO

-- Admin Settle Withdrawal Request (Automatic Payout Settle or manual)
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

            -- Ledger Debit Entry
            INSERT INTO FinancialLedger (BookingId, GurujiId, Amount, GurujiShare, TrustShare, Type, Status)
            VALUES ('WITHDRAWAL_' + CAST(@Id AS NVARCHAR(10)), @GurujiId, @Amount, @Amount, 0.00, 'Withdrawal', 'Success');
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

-- ==========================================
-- ६. FEEDBACKS (फीडबॅक) PROCEDURES
-- ==========================================

-- Insert feedback
CREATE PROCEDURE sp_Feedback_Insert
    @BookingId NVARCHAR(128),
    @UserId NVARCHAR(128),
    @GurujiId NVARCHAR(128),
    @Rating INT,
    @Comment NVARCHAR(1000),
    @Sentiment NVARCHAR(20)
AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO Feedbacks (BookingId, UserId, GurujiId, Rating, Comment, Sentiment)
    VALUES (@BookingId, @UserId, @GurujiId, @Rating, @Comment, @Sentiment);
    
    -- Save FeedbackId references inside bookings table
    -- Since we use composite query, the feedbacks are fetched on demand
END;
GO

-- ==========================================
-- ७. SYSTEM SETTINGS PROCEDURES
-- ==========================================

-- Get System Config value
CREATE PROCEDURE sp_Settings_Get
    @Key VARCHAR(100)
AS
BEGIN
    SET NOCOUNT ON;
    SELECT SettingValue FROM SystemSettings WHERE SettingKey = @Key;
END;
GO

-- Update System Config
CREATE PROCEDURE sp_Settings_Update
    @Key VARCHAR(100),
    @Value NVARCHAR(MAX)
AS
BEGIN
    SET NOCOUNT ON;
    MERGE SystemSettings AS Target
    USING (SELECT @Key AS SettingKey) AS Source
    ON (Target.SettingKey = Source.SettingKey)
    WHEN MATCHED THEN
        UPDATE SET SettingValue = @Value
    WHEN NOT MATCHED THEN
        INSERT (SettingKey, SettingValue) VALUES (@Key, @Value);
END;
GO

-- ==========================================
-- ⚠️ ८. SECURITY & RATE LIMITS
-- ==========================================

-- Rate Limit checking transaction function
CREATE PROCEDURE sp_RateLimit_CheckAndIncrement
    @Scope VARCHAR(100),
    @Subject VARCHAR(100),
    @Limit INT,
    @WindowMs INT, -- duration of the rate-limit window in milliseconds
    @IsBlocked BIT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @WindowStart DATETIME2;
    DECLARE @NowMs BIGINT = DATEDIFF(SECOND, '1970-01-01', GETUTCDATE()) * 1000;
    
    -- Normalise window started time
    DECLARE @WindowStartMs BIGINT = (@NowMs / @WindowMs) * @WindowMs;
    SET @WindowStart = DATEADD(MILLISECOND, @WindowStartMs % 1000, DATEADD(SECOND, @WindowStartMs / 1000, '1970-01-01'));

    BEGIN TRANSACTION;
    BEGIN TRY
        IF EXISTS (SELECT 1 FROM SystemRateLimits WHERE Scope = @Scope AND Subject = @Subject AND WindowStartedAt = @WindowStart)
        BEGIN
            DECLARE @CurrentCount INT;
            SELECT @CurrentCount = RequestCount FROM SystemRateLimits WHERE Scope = @Scope AND Subject = @Subject AND WindowStartedAt = @WindowStart;

            IF @CurrentCount >= @Limit
            BEGIN
                SET @IsBlocked = 1;
            END
            ELSE
            BEGIN
                UPDATE SystemRateLimits
                SET RequestCount = RequestCount + 1
                WHERE Scope = @Scope AND Subject = @Subject AND WindowStartedAt = @WindowStart;
                SET @IsBlocked = 0;
            END
        END
        ELSE
        BEGIN
            INSERT INTO SystemRateLimits (Scope, Subject, RequestCount, WindowStartedAt)
            VALUES (@Scope, @Subject, 1, @WindowStart);
            SET @IsBlocked = 0;
        END

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END;
GO
