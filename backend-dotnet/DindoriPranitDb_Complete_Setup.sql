-- =====================================================================
-- DINDORI PRANIT YADNYIKI & VASTU SEVA
-- COMPLETE 1-TO-1 DATABASE SCHEMA (MS SQL SERVER)
-- Replicates every single Firebase Firestore collection exactly as-is
-- Simplified only for Admin (single owner) and Maker-Checker per instructions
-- =====================================================================

-- 1. Users Table (users collection)
CREATE TABLE Users (
    Uid NVARCHAR(128) PRIMARY KEY,
    FullName NVARCHAR(255) NOT NULL,
    Mobile NVARCHAR(20) NOT NULL UNIQUE,
    Email NVARCHAR(255) NULL,
    Address NVARCHAR(500) NULL,
    District NVARCHAR(100) NULL,
    Pincode NVARCHAR(10) NULL,
    Lat FLOAT NOT NULL DEFAULT 0.0,
    Lng FLOAT NOT NULL DEFAULT 0.0,
    FcmToken NVARCHAR(500) NULL,
    Status NVARCHAR(20) NOT NULL DEFAULT 'Active' CHECK (Status IN ('Active', 'Inactive', 'Blocked', 'Deleted')),
    CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    UpdatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE()
);
CREATE INDEX IX_Users_Mobile ON Users(Mobile);

-- 2. Guruji Table (guruji collection)
CREATE TABLE Guruji (
    Uid NVARCHAR(128) PRIMARY KEY,
    FullName NVARCHAR(255) NOT NULL,
    Mobile NVARCHAR(20) NOT NULL UNIQUE,
    Email NVARCHAR(255) NULL,
    Address NVARCHAR(500) NULL,
    District NVARCHAR(100) NULL,
    Pincode NVARCHAR(10) NULL,
    Lat FLOAT NOT NULL DEFAULT 0.0,
    Lng FLOAT NOT NULL DEFAULT 0.0,
    Expertises NVARCHAR(MAX) NOT NULL,
    WalletBalance DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    TotalEarnings DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    PendingWithdrawal DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    IsAvailable BIT NOT NULL DEFAULT 0,
    FcmToken NVARCHAR(500) NULL,
    Status NVARCHAR(20) NOT NULL DEFAULT 'Pending' CHECK (Status IN ('Pending', 'Approved', 'Active', 'Inactive', 'Blocked', 'Rejected')),
    ExpertType NVARCHAR(20) NOT NULL DEFAULT 'Guruji' CHECK (ExpertType IN ('Guruji', 'VastuExpert')),
    CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    UpdatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE()
);
CREATE INDEX IX_Guruji_Mobile ON Guruji(Mobile);

-- 3. Admins Table (admins collection)
CREATE TABLE Admins (
    Uid NVARCHAR(128) PRIMARY KEY,
    Email NVARCHAR(255) NOT NULL UNIQUE,
    Status NVARCHAR(20) NOT NULL DEFAULT 'Active' CHECK (Status IN ('Active', 'Blocked')),
    CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE()
);

-- 4. Poojas Table (poojas collection)
CREATE TABLE Poojas (
    Id NVARCHAR(50) PRIMARY KEY,
    Name NVARCHAR(255) NOT NULL,
    Category NVARCHAR(100) NOT NULL,
    BasePrice DECIMAL(18,2) NOT NULL,
    SevaType NVARCHAR(20) NOT NULL DEFAULT 'Yadnyiki' CHECK (SevaType IN ('Yadnyiki', 'Vastu')),
    Status NVARCHAR(20) NOT NULL DEFAULT 'Active' CHECK (Status IN ('Active', 'Inactive', 'Deleted'))
);

-- 5. Bookings Table (bookings collection)
CREATE TABLE Bookings (
    Id NVARCHAR(128) PRIMARY KEY,
    DisplayId NVARCHAR(50) NOT NULL UNIQUE,
    UserId NVARCHAR(128) NOT NULL FOREIGN KEY REFERENCES Users(Uid),
    GurujiId NVARCHAR(128) NULL FOREIGN KEY REFERENCES Guruji(Uid),
    PoojaId NVARCHAR(50) NOT NULL FOREIGN KEY REFERENCES Poojas(Id),
    Date DATE NOT NULL,
    Status NVARCHAR(30) NOT NULL DEFAULT 'Pending' CHECK (Status IN ('Pending', 'Assigned', 'Accepted', 'InProgress', 'PaymentPending', 'Completed', 'Cancelled')),
    PaymentStatus NVARCHAR(30) NOT NULL DEFAULT 'Pending' CHECK (PaymentStatus IN ('Pending', 'OrderCreated', 'Submitted', 'Paid', 'Rejected')),
    Amount DECIMAL(18,2) NOT NULL,
    GurujiShare DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    TrustShare DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    ContactName NVARCHAR(255) NOT NULL,
    ContactPhone NVARCHAR(20) NOT NULL,
    Address NVARCHAR(500) NOT NULL,
    District NVARCHAR(100) NOT NULL,
    Pincode NVARCHAR(10) NOT NULL,
    UserLat FLOAT NOT NULL,
    UserLng FLOAT NOT NULL,
    RazorpayOrderId NVARCHAR(100) NULL,
    RazorpayPaymentId NVARCHAR(100) NULL,
    OtpHash NVARCHAR(500) NULL,
    OtpExpiresAt DATETIME2 NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    UpdatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE()
);
CREATE INDEX IX_Bookings_UserId ON Bookings(UserId);
CREATE INDEX IX_Bookings_GurujiId ON Bookings(GurujiId);

-- 6. BookingEvents Table (booking_events collection)
CREATE TABLE BookingEvents (
    Id INT IDENTITY(1,1) PRIMARY KEY,
    BookingId NVARCHAR(128) NOT NULL FOREIGN KEY REFERENCES Bookings(Id) ON DELETE CASCADE,
    Type NVARCHAR(50) NOT NULL,
    Status NVARCHAR(30) NOT NULL,
    ActorId NVARCHAR(128) NOT NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE()
);

-- 7. BookingOtps Table (booking_otps collection)
CREATE TABLE BookingOtps (
    BookingId NVARCHAR(128) PRIMARY KEY FOREIGN KEY REFERENCES Bookings(Id) ON DELETE CASCADE,
    OtpHash NVARCHAR(500) NOT NULL,
    OtpSalt NVARCHAR(500) NOT NULL,
    ExpiresAt DATETIME2 NOT NULL
);

-- 8. BookingAssignmentLocks Table (booking_assignment_locks collection)
CREATE TABLE BookingAssignmentLocks (
    LockDate DATE NOT NULL,
    GurujiId NVARCHAR(128) NOT NULL FOREIGN KEY REFERENCES Guruji(Uid),
    BookingId NVARCHAR(128) NOT NULL FOREIGN KEY REFERENCES Bookings(Id),
    CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    PRIMARY KEY (LockDate, GurujiId)
);

-- 9. BookingIdempotency Table (booking_idempotency collection)
CREATE TABLE BookingIdempotency (
    UserId NVARCHAR(128) NOT NULL FOREIGN KEY REFERENCES Users(Uid),
    ClientRequestId NVARCHAR(128) NOT NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    PRIMARY KEY (UserId, ClientRequestId)
);

-- 10. ServiceRequests Table (service_requests collection)
CREATE TABLE ServiceRequests (
    Id INT IDENTITY(1,1) PRIMARY KEY,
    BookingId NVARCHAR(128) NOT NULL FOREIGN KEY REFERENCES Bookings(Id) ON DELETE CASCADE,
    Type NVARCHAR(20) NOT NULL CHECK (Type IN ('Reschedule', 'Cancel')),
    Status NVARCHAR(20) NOT NULL DEFAULT 'Pending' CHECK (Status IN ('Pending', 'Approved', 'Rejected')),
    RequestedDate DATE NULL,
    Reason NVARCHAR(500) NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE()
);

-- 11. FinancialLedger Table (financial_ledger collection)
CREATE TABLE FinancialLedger (
    Id INT IDENTITY(1,1) PRIMARY KEY,
    BookingId NVARCHAR(128) NOT NULL FOREIGN KEY REFERENCES Bookings(Id),
    GurujiId NVARCHAR(128) NOT NULL FOREIGN KEY REFERENCES Guruji(Uid),
    Amount DECIMAL(18,2) NOT NULL,
    GurujiShare DECIMAL(18,2) NOT NULL,
    TrustShare DECIMAL(18,2) NOT NULL,
    Type NVARCHAR(20) NOT NULL CHECK (Type IN ('Credit', 'Debit', 'Payment', 'Withdrawal', 'Refund')),
    Status NVARCHAR(20) NOT NULL CHECK (Status IN ('Pending', 'Success', 'Failed')),
    CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE()
);

-- 12. ReceiptSnapshots Table (receipt_snapshots collection)
CREATE TABLE ReceiptSnapshots (
    BookingId NVARCHAR(128) PRIMARY KEY FOREIGN KEY REFERENCES Bookings(Id) ON DELETE CASCADE,
    ReceiptNo NVARCHAR(100) NOT NULL UNIQUE,
    Amount DECIMAL(18,2) NOT NULL,
    PaymentStatus NVARCHAR(30) NOT NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE()
);

-- 13. WithdrawalRequests Table (withdrawal_requests collection)
CREATE TABLE WithdrawalRequests (
    Id INT IDENTITY(1,1) PRIMARY KEY,
    GurujiId NVARCHAR(128) NOT NULL FOREIGN KEY REFERENCES Guruji(Uid),
    Amount DECIMAL(18,2) NOT NULL,
    BankAccount NVARCHAR(50) NULL,
    IFSC NVARCHAR(20) NULL,
    UPI NVARCHAR(100) NULL,
    Status NVARCHAR(30) NOT NULL DEFAULT 'Pending' CHECK (Status IN ('Pending', 'Processing', 'Settled', 'Failed', 'ReconciliationRequired')),
    SettlementRef NVARCHAR(100) NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE()
);

-- 14. Feedbacks Table (feedbacks collection)
CREATE TABLE Feedbacks (
    Id INT IDENTITY(1,1) PRIMARY KEY,
    BookingId NVARCHAR(128) NOT NULL FOREIGN KEY REFERENCES Bookings(Id),
    UserId NVARCHAR(128) NOT NULL FOREIGN KEY REFERENCES Users(Uid),
    GurujiId NVARCHAR(128) NOT NULL FOREIGN KEY REFERENCES Guruji(Uid),
    Rating INT NOT NULL CHECK (Rating BETWEEN 1 AND 5),
    Comment NVARCHAR(1000) NULL,
    Sentiment NVARCHAR(20) NULL CHECK (Sentiment IN ('Positive', 'Neutral', 'Negative')),
    CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE()
);

-- 15. SupportTickets Table (support_tickets collection)
CREATE TABLE SupportTickets (
    Id NVARCHAR(128) PRIMARY KEY,
    UserId NVARCHAR(128) NOT NULL,
    Subject NVARCHAR(255) NOT NULL,
    Description NVARCHAR(MAX) NOT NULL,
    Language NVARCHAR(50) NOT NULL DEFAULT 'mr',
    Category NVARCHAR(100) NOT NULL,
    Status NVARCHAR(20) NOT NULL DEFAULT 'Open' CHECK (Status IN ('Open', 'In Progress', 'Resolved', 'Closed')),
    Priority NVARCHAR(20) NOT NULL DEFAULT 'Low' CHECK (Priority IN ('Low', 'Medium', 'High', 'Critical')),
    TriageCategory NVARCHAR(100) NULL,
    TriageSeverity NVARCHAR(20) NULL,
    SuggestedFAQs NVARCHAR(MAX) NULL,
    SuggestedSOPs NVARCHAR(MAX) NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    UpdatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE()
);
CREATE INDEX IX_SupportTickets_UserId ON SupportTickets(UserId);

-- 16. UserNotifications Table (user_notifications collection)
CREATE TABLE UserNotifications (
    Id INT IDENTITY(1,1) PRIMARY KEY,
    UserId NVARCHAR(128) NOT NULL,
    Title NVARCHAR(255) NOT NULL,
    Body NVARCHAR(1000) NOT NULL,
    Action NVARCHAR(100) NULL,
    BookingId NVARCHAR(128) NULL,
    DeepLink NVARCHAR(500) NULL,
    Priority NVARCHAR(20) NOT NULL DEFAULT 'Normal',
    IsRead BIT NOT NULL DEFAULT 0,
    CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE()
);
CREATE INDEX IX_UserNotifications_UserId ON UserNotifications(UserId);

-- 17. ScheduledNotifications Table (scheduled_notifications collection)
CREATE TABLE ScheduledNotifications (
    Id INT IDENTITY(1,1) PRIMARY KEY,
    Channel NVARCHAR(50) NOT NULL DEFAULT 'fcm_user',
    TargetUserId NVARCHAR(128) NULL,
    TargetTopic NVARCHAR(100) NULL,
    Title NVARCHAR(255) NOT NULL,
    Body NVARCHAR(1000) NOT NULL,
    Action NVARCHAR(100) NULL,
    BookingId NVARCHAR(128) NULL,
    ScheduledTime DATETIME2 NOT NULL,
    Status NVARCHAR(20) NOT NULL DEFAULT 'Scheduled' CHECK (Status IN ('Scheduled', 'Sent', 'Failed')),
    DeliveryState NVARCHAR(20) NOT NULL DEFAULT 'queued',
    RetryCount INT NOT NULL DEFAULT 0,
    FailoverState NVARCHAR(50) NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    UpdatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE()
);

-- 18. NotificationLogs Table (notification_logs collection)
CREATE TABLE NotificationLogs (
    Id INT IDENTITY(1,1) PRIMARY KEY,
    Action NVARCHAR(100) NOT NULL,
    BookingId NVARCHAR(128) NULL,
    Target NVARCHAR(255) NOT NULL,
    Status NVARCHAR(20) NOT NULL,
    Channel NVARCHAR(50) NOT NULL,
    FailureReason NVARCHAR(500) NULL,
    RetryCount INT NOT NULL DEFAULT 0,
    FailoverState NVARCHAR(50) NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE()
);

-- 19. AdminRiskEvents Table (admin_risk_events collection)
CREATE TABLE AdminRiskEvents (
    Id INT IDENTITY(1,1) PRIMARY KEY,
    Type NVARCHAR(100) NOT NULL,
    Severity NVARCHAR(20) NOT NULL CHECK (Severity IN ('low', 'medium', 'high', 'critical')),
    Status NVARCHAR(20) NOT NULL DEFAULT 'Open' CHECK (Status IN ('Open', 'Processing', 'Resolved')),
    BookingId NVARCHAR(128) NULL,
    GurujiId NVARCHAR(128) NULL,
    TicketId NVARCHAR(128) NULL,
    Message NVARCHAR(1000) NOT NULL,
    EscalationState NVARCHAR(50) NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    UpdatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE()
);

-- 20. AdminNotifications Table (admin_notifications collection)
CREATE TABLE AdminNotifications (
    Id INT IDENTITY(1,1) PRIMARY KEY,
    Type NVARCHAR(100) NOT NULL,
    BookingId NVARCHAR(128) NULL,
    GurujiId NVARCHAR(128) NULL,
    Message NVARCHAR(1000) NOT NULL,
    Status NVARCHAR(20) NOT NULL DEFAULT 'Open' CHECK (Status IN ('Open', 'Resolved')),
    CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE()
);

-- 21. AdminClientErrors Table (admin_client_errors collection)
CREATE TABLE AdminClientErrors (
    Id INT IDENTITY(1,1) PRIMARY KEY,
    Source NVARCHAR(100) NOT NULL,
    Level NVARCHAR(20) NOT NULL,
    Message NVARCHAR(MAX) NOT NULL,
    Page NVARCHAR(255) NULL,
    Stack NVARCHAR(MAX) NULL,
    Metadata NVARCHAR(MAX) NULL, -- JSON formatted string
    AdminId NVARCHAR(128) NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE()
);

-- 22. AuditLogs Table (audit_logs collection)
CREATE TABLE AuditLogs (
    Id INT IDENTITY(1,1) PRIMARY KEY,
    AdminId NVARCHAR(128) NOT NULL,
    AdminEmail NVARCHAR(255) NULL,
    AdminName NVARCHAR(255) NULL,
    Action NVARCHAR(100) NOT NULL,
    Module NVARCHAR(100) NOT NULL,
    TargetId NVARCHAR(128) NULL,
    Details NVARCHAR(MAX) NOT NULL,
    Status NVARCHAR(20) NOT NULL DEFAULT 'Success',
    CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE()
);

-- 23. DeadLetters Table (dead_letters / dead_letter_queue collection)
CREATE TABLE DeadLetters (
    Id INT IDENTITY(1,1) PRIMARY KEY,
    Domain NVARCHAR(100) NOT NULL,
    Type NVARCHAR(100) NOT NULL,
    EntityId NVARCHAR(128) NULL,
    Payload NVARCHAR(MAX) NOT NULL, -- Store raw JSON
    Error NVARCHAR(MAX) NOT NULL, -- Store raw JSON
    Severity NVARCHAR(20) NOT NULL DEFAULT 'medium',
    Status NVARCHAR(20) NOT NULL DEFAULT 'Pending' CHECK (Status IN ('Pending', 'Replayed', 'Failed')),
    Reason NVARCHAR(500) NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    UpdatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE()
);

-- 24. PaymentFailures Table (payment_failures collection)
CREATE TABLE PaymentFailures (
    Id INT IDENTITY(1,1) PRIMARY KEY,
    PaymentId NVARCHAR(100) NOT NULL,
    OrderId NVARCHAR(100) NOT NULL,
    BookingId NVARCHAR(128) NOT NULL,
    Amount DECIMAL(18,2) NOT NULL,
    Method NVARCHAR(50) NULL,
    Error NVARCHAR(MAX) NOT NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE()
);

-- 25. PaymentDisputes Table (payment_disputes collection)
CREATE TABLE PaymentDisputes (
    Id NVARCHAR(100) PRIMARY KEY,
    PaymentId NVARCHAR(100) NOT NULL,
    Amount DECIMAL(18,2) NOT NULL,
    Currency NVARCHAR(10) NOT NULL,
    Status NVARCHAR(50) NOT NULL,
    Reason NVARCHAR(255) NULL,
    Payload NVARCHAR(MAX) NOT NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE()
);

-- 26. PaymentDowntimes Table (payment_downtime collection)
CREATE TABLE PaymentDowntimes (
    Id INT IDENTITY(1,1) PRIMARY KEY,
    Method NVARCHAR(50) NOT NULL,
    CardNetwork NVARCHAR(50) NULL,
    Status NVARCHAR(50) NOT NULL,
    StartTime DATETIME2 NOT NULL,
    EndTime DATETIME2 NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE()
);

-- 27. RazorpaySettlements Table (razorpay_settlements collection)
CREATE TABLE RazorpaySettlements (
    Id NVARCHAR(100) PRIMARY KEY,
    Amount DECIMAL(18,2) NOT NULL,
    Tax DECIMAL(18,2) NOT NULL,
    Ondemand BIT NOT NULL DEFAULT 0,
    SettledAt DATETIME2 NOT NULL,
    Payload NVARCHAR(MAX) NOT NULL
);

-- 28. RazorpayWebhookEvents Table (razorpay_webhook_events collection)
CREATE TABLE RazorpayWebhookEvents (
    EventId NVARCHAR(100) PRIMARY KEY,
    EventName NVARCHAR(100) NOT NULL,
    OutcomeState NVARCHAR(20) NOT NULL,
    OutcomeReason NVARCHAR(500) NULL,
    ProcessedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE()
);

-- 29. SystemBackups Table (system_backups collection)
CREATE TABLE SystemBackups (
    Id INT IDENTITY(1,1) PRIMARY KEY,
    Schedule NVARCHAR(50) NOT NULL,
    FileName NVARCHAR(255) NOT NULL,
    DownloadUrl NVARCHAR(500) NOT NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE()
);

-- 30. ActivityTimeline Table (activity_timeline collection)
CREATE TABLE ActivityTimeline (
    Id INT IDENTITY(1,1) PRIMARY KEY,
    EntityType NVARCHAR(50) NOT NULL,
    EntityId NVARCHAR(128) NOT NULL,
    EventType NVARCHAR(50) NOT NULL,
    Status NVARCHAR(50) NOT NULL,
    Message NVARCHAR(1000) NOT NULL,
    BookingId NVARCHAR(128) NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE()
);

-- 31. BotActivityLogs Table (bot_activity_logs collection)
CREATE TABLE BotActivityLogs (
    Id INT IDENTITY(1,1) PRIMARY KEY,
    Action NVARCHAR(100) NOT NULL,
    Status NVARCHAR(50) NOT NULL,
    Details NVARCHAR(MAX) NOT NULL, -- Store raw JSON
    CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE()
);

-- 32. SystemSettings Table (settings / system collections)
CREATE TABLE SystemSettings (
    SettingKey VARCHAR(100) PRIMARY KEY,
    SettingValue NVARCHAR(MAX) NOT NULL
);

-- 33. SystemRateLimits Table (system_rate_limits collection)
CREATE TABLE SystemRateLimits (
    Scope VARCHAR(100) NOT NULL,
    Subject VARCHAR(100) NOT NULL,
    RequestCount INT NOT NULL DEFAULT 1,
    WindowStartedAt DATETIME2 NOT NULL,
    PRIMARY KEY (Scope, Subject, WindowStartedAt)
);
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
-- =====================================================================
-- DINDORI PRANIT YADNYIKI & VASTU SEVA
-- INITIAL SEED DATA (MS SQL SERVER)
-- Populates the default Pooja lists and System Settings
-- =====================================================================

-- 1. Insert Default Pooja List (यज्ञिकी विभाग)
INSERT INTO Poojas (Id, Name, Category, BasePrice, SevaType, Status) VALUES
('satyanarayan_pooja', N'श्री सत्यनारायण महापूजा', N'कौटुंबिक पूजा', 1100.00, 'Yadnyiki', 'Active'),
('ganpati_pooja', N'श्री गणेश पूजा व अभिषेक', N'कौटुंबिक पूजा', 1500.00, 'Yadnyiki', 'Active'),
('vastushanti_vidhi', N'वास्तूशांत विधी व होम', N'गृह शांत विधी', 5100.00, 'Yadnyiki', 'Active'),
('rudrabhishek_havan', N'लघु रुद्र व हव्हन विधी', N'यज्ञ विधी', 7500.00, 'Yadnyiki', 'Active'),
('navchandi_yajna', N'नवचंडी महायज्ञ विधी', N'महायज्ञ विधी', 15000.00, 'Yadnyiki', 'Active'),
('lagna_vidhi', N'विवाह संस्कार विधी', N'संस्कार विधी', 5500.00, 'Yadnyiki', 'Active');

-- 2. Insert Default Vastu Sevas (वास्तू सेवा विभाग)
INSERT INTO Poojas (Id, Name, Category, BasePrice, SevaType, Status) VALUES
('vastu_tapasani_flat', N'निवासी वास्तू तपासणी (Flat/Bungalow)', N'वास्तू तपासणी', 2100.00, 'Vastu', 'Active'),
('vastu_tapasani_comm', N'व्यावसायिक वास्तू तपासणी (Shop/Office)', N'वास्तू तपासणी', 3100.00, 'Vastu', 'Active'),
('vastu_tapasani_ind', N'औद्योगिक वास्तू तपासणी (Factory)', N'वास्तू तपासणी', 5100.00, 'Vastu', 'Active'),
('vastu_upay_nodemolish', N'तोडफोड विना वास्तू दोष निवारण उपाय', N'वास्तू दोष उपाय', 1500.00, 'Vastu', 'Active');

-- 3. Insert Default System Settings (सिस्टीम पॉलिसी आणि कमिशन रेशो)
-- Default Share split split: Trust 70%, Guruji 30%
INSERT INTO SystemSettings (SettingKey, SettingValue) VALUES
('financial_config', N'{"trustSharePercent":70.0,"gurujiSharePercent":30.0,"tdsPercent":5.0,"razorpayFeePercent":2.36}'),
('system_config', N'{"maintenanceMode":false,"killSwitchEnabled":false,"disableNewBookings":false,"autoApproveServiceRequests":true,"autoProcessLowRiskWithdrawals":true}');

-- 4. Create Default Admin Owner Whitelist Account
-- (Replace this email with your actual Admin Email address)
INSERT INTO Admins (Uid, Email, Status) VALUES
('admin_owner_uid_placeholder', 'admin@dindori.org', 'Active');
-- =====================================================================
-- DINDORI PRANIT YADNYIKI & VASTU SEVA
-- NEW STORED PROCEDURES FOR SUPPORT TICKETS AND NOTIFICATIONS INBOX
-- =====================================================================

-- 1. Create Support Ticket
CREATE PROCEDURE sp_SupportTicket_Create
    @Id NVARCHAR(128),
    @UserId NVARCHAR(128),
    @Subject NVARCHAR(255),
    @Description NVARCHAR(MAX),
    @Category NVARCHAR(100) = 'General',
    @Language NVARCHAR(50) = 'mr'
AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO SupportTickets (Id, UserId, Subject, Description, Category, Language, Status, Priority, CreatedAt, UpdatedAt)
    VALUES (@Id, @UserId, @Subject, @Description, @Category, @Language, 'Open', 'Low', GETUTCDATE(), GETUTCDATE());
END;
GO

-- 2. Get Support Tickets by User ID
CREATE PROCEDURE sp_SupportTicket_GetByUserId
    @UserId NVARCHAR(128)
AS
BEGIN
    SET NOCOUNT ON;
    SELECT Id, UserId, Subject, Description, Category, Language, Status, Priority, CreatedAt, UpdatedAt
    FROM SupportTickets
    WHERE UserId = @UserId
    ORDER BY CreatedAt DESC;
END;
GO

-- 3. Get Notifications by User ID
CREATE PROCEDURE sp_UserNotification_GetByUserId
    @UserId NVARCHAR(128)
AS
BEGIN
    SET NOCOUNT ON;
    SELECT Id, UserId, Title, Body, Action, BookingId, DeepLink, Priority, IsRead, CreatedAt
    FROM UserNotifications
    WHERE UserId = @UserId
    ORDER BY CreatedAt DESC;
END;
GO

-- 4. Mark Notification as Read
CREATE PROCEDURE sp_UserNotification_MarkRead
    @Id INT,
    @UserId NVARCHAR(128)
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE UserNotifications
    SET IsRead = 1
    WHERE Id = @Id AND UserId = @UserId;
END;
GO

-- 5. Create Booking Service Request (Cancel / Reschedule)
CREATE PROCEDURE sp_ServiceRequest_Create
    @BookingId NVARCHAR(128),
    @Type NVARCHAR(20),
    @RequestedDate DATE = NULL,
    @Reason NVARCHAR(500) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO ServiceRequests (BookingId, Type, Status, RequestedDate, Reason, CreatedAt)
    VALUES (@BookingId, @Type, 'Pending', @RequestedDate, @Reason, GETUTCDATE());
END;
GO
-- =====================================================================
-- DINDORI PRANIT YADNYIKI - ENTERPRISE UPDATES SCHEMA (MS SQL SERVER)
-- Adds KYC, Advanced Auditing, Web Support Chat, and Accounting Webhook tables/procs
-- =====================================================================

-- 1. Alter Guruji Table to add KYC fields
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Guruji') AND name = 'PanCardUrl')
BEGIN
    ALTER TABLE Guruji ADD 
        PanCardUrl NVARCHAR(500) NULL,
        AadharCardUrl NVARCHAR(500) NULL,
        KycStatus NVARCHAR(30) NOT NULL DEFAULT 'Pending' CHECK (KycStatus IN ('Pending', 'Submitted', 'Approved', 'Rejected')),
        KycVerifiedAt DATETIME2 NULL;
END
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Guruji') AND name = 'PanNumber')
BEGIN
    ALTER TABLE Guruji ADD 
        PanNumber NVARCHAR(20) NULL,
        AadharNumber NVARCHAR(20) NULL;
END
GO

-- 2. Alter AuditLogs Table to add advanced logging fields
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('AuditLogs') AND name = 'ClientIp')
BEGIN
    ALTER TABLE AuditLogs ADD 
        ClientIp NVARCHAR(50) NULL,
        RequestPayload NVARCHAR(MAX) NULL,
        ThreatLevel NVARCHAR(20) NOT NULL DEFAULT 'Low' CHECK (ThreatLevel IN ('Low', 'Medium', 'High', 'Critical'));
END
GO

-- 3. Create ChatMessages Table (for live support and chatbot conversations)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'ChatMessages')
BEGIN
    CREATE TABLE ChatMessages (
        Id INT IDENTITY(1,1) PRIMARY KEY,
        SenderId NVARCHAR(128) NOT NULL,
        SenderName NVARCHAR(255) NOT NULL,
        ReceiverId NVARCHAR(128) NOT NULL, -- Can be 'Admin' or specific user/guruji UID
        Message NVARCHAR(MAX) NOT NULL,
        IsRead BIT NOT NULL DEFAULT 0,
        CreatedAt DATETIME2 NOT NULL DEFAULT GETUTCDATE()
    );
    CREATE INDEX IX_ChatMessages_SenderId ON ChatMessages(SenderId);
    CREATE INDEX IX_ChatMessages_ReceiverId ON ChatMessages(ReceiverId);
END
GO

-- 4. Create WebhookSettings Table (for Tally / Accounting webhook urls)
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
GO

-- =====================================================================
-- STORED PROCEDURES
-- =====================================================================

-- 5. Stored Procedure: Update Guruji KYC
IF EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID('sp_Guruji_UpdateKyc') AND type = 'P')
    DROP PROCEDURE sp_Guruji_UpdateKyc;
GO
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
END
GO

-- 6. Stored Procedure: Insert Chat Message
IF EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID('sp_ChatMessage_Insert') AND type = 'P')
    DROP PROCEDURE sp_ChatMessage_Insert;
GO
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
END
GO

-- 7. Stored Procedure: Get Chat History
IF EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID('sp_ChatMessage_GetHistory') AND type = 'P')
    DROP PROCEDURE sp_ChatMessage_GetHistory;
GO
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
END
GO

-- 8. Stored Procedure: Save Webhook Setting
IF EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID('sp_WebhookSettings_Save') AND type = 'P')
    DROP PROCEDURE sp_WebhookSettings_Save;
GO
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
END
GO

-- 9. Stored Procedure: Advanced Audit Log Insert
IF EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID('sp_AuditLog_Insert_Advanced') AND type = 'P')
    DROP PROCEDURE sp_AuditLog_Insert_Advanced;
GO
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
END
GO
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
-- =====================================================================
-- DINDORI PRANIT YADNYIKI - ENTERPRISE UPGRADES SCHEMA 07
-- Adds HomeLocation columns and alters Booking Status check constraint
-- =====================================================================

-- 1. Alter Guruji Table to add HomeLat and HomeLng
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Guruji') AND name = 'HomeLat')
BEGIN
    ALTER TABLE Guruji ADD 
        HomeLat FLOAT NOT NULL DEFAULT 0.0,
        HomeLng FLOAT NOT NULL DEFAULT 0.0;
END
GO

-- Sync existing coordinates to HomeLocation
UPDATE Guruji 
SET HomeLat = Lat, HomeLng = Lng 
WHERE HomeLat = 0.0 AND Lat <> 0.0;
GO

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
GO

IF NOT EXISTS (SELECT * FROM sys.objects WHERE parent_object_id = OBJECT_ID('Bookings') AND name = 'CK_Bookings_Status' AND type = 'C')
BEGIN
    ALTER TABLE Bookings ADD CONSTRAINT CK_Bookings_Status 
    CHECK (Status IN ('Pending', 'Assigned', 'Accepted', 'Departed', 'Arrived', 'InProgress', 'PaymentPending', 'Completed', 'Cancelled'));
END
GO
-- =====================================================================
-- DINDORI PRANIT YADNYIKI - ENTERPRISE UPGRADES SCHEMA 08
-- Adds Email Notifications Queue Table
-- =====================================================================

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
    CREATE INDEX IX_EmailQueue_Status ON EmailQueue(Status);
END
GO
-- =====================================================================
-- DINDORI PRANIT YADNYIKI - ENTERPRISE UPGRADES SCHEMA 09
-- Adds Guruji Availability Roster / Muhurt Calendar Matching
-- And Stored Procedures for Availability and Transactions
-- =====================================================================

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
GO

-- 1. Procedure to Bulk Save/Sync Guruji Availability Dates (using comma-separated string)
CREATE OR ALTER PROCEDURE sp_GurujiAvailability_Save
    @GurujiId NVARCHAR(128),
    @DateList NVARCHAR(MAX)
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRANSACTION;
    BEGIN TRY
        -- Delete all future availability dates for this Guruji first
        DELETE FROM GurujiAvailability 
        WHERE GurujiId = @GurujiId 
          AND AvailableDate >= CAST(GETUTCDATE() AS DATE);

        -- Bulk Insert new dates from comma-separated list using STRING_SPLIT
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
GO

-- 2. Procedure to Retrieve Guruji's saved availability dates
CREATE OR ALTER PROCEDURE sp_GurujiAvailability_Get
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
GO

-- 3. Procedure for Yajman to fetch aggregated available dates (Union of all active Gurujis)
CREATE OR ALTER PROCEDURE sp_Booking_GetAvailableDates
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
GO

-- 4. Procedure to get Guruji's wallet transactions history
CREATE OR ALTER PROCEDURE sp_Guruji_GetTransactions
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
GO
