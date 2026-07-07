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
