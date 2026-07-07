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
