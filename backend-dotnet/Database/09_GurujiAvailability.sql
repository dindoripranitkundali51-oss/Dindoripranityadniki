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
