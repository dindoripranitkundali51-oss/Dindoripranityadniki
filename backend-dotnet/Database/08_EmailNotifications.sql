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
