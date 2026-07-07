-- Add PasswordHash column to tables if they don't exist
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Users') AND name = 'PasswordHash')
BEGIN
    ALTER TABLE Users ADD PasswordHash NVARCHAR(500) NULL;
END
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Guruji') AND name = 'PasswordHash')
BEGIN
    ALTER TABLE Guruji ADD PasswordHash NVARCHAR(500) NULL;
END
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Admins') AND name = 'PasswordHash')
BEGIN
    ALTER TABLE Admins ADD PasswordHash NVARCHAR(500) NULL;
END
GO

-- Create LoginOtps Table for Login OTP verification
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID('LoginOtps') AND type = 'U')
BEGIN
    CREATE TABLE LoginOtps (
        Mobile NVARCHAR(100) PRIMARY KEY,
        OtpHash NVARCHAR(500) NOT NULL,
        ExpiresAt DATETIME2 NOT NULL
    );
END
GO

-- Re-create / Alter sp_User_Insert Stored Procedure
ALTER PROCEDURE sp_User_Insert
    @Uid NVARCHAR(128),
    @FullName NVARCHAR(255),
    @Mobile NVARCHAR(20),
    @Email NVARCHAR(255) = NULL,
    @Address NVARCHAR(500) = NULL,
    @District NVARCHAR(100) = NULL,
    @Pincode NVARCHAR(10) = NULL,
    @Lat FLOAT = 0.0,
    @Lng FLOAT = 0.0,
    @PasswordHash NVARCHAR(500) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO Users (Uid, FullName, Mobile, Email, Address, District, Pincode, Lat, Lng, PasswordHash)
    VALUES (@Uid, @FullName, @Mobile, @Email, @Address, @District, @Pincode, @Lat, @Lng, @PasswordHash);
END;
GO

-- Re-create / Alter sp_Guruji_Insert Stored Procedure
ALTER PROCEDURE sp_Guruji_Insert
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
    @ExpertType NVARCHAR(20),
    @PasswordHash NVARCHAR(500) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO Guruji (Uid, FullName, Mobile, Email, Address, District, Pincode, Lat, Lng, Expertises, ExpertType, Status, PasswordHash)
    VALUES (@Uid, @FullName, @Mobile, @Email, @Address, @District, @Pincode, @Lat, @Lng, @Expertises, @ExpertType, 'Pending', @PasswordHash);
END;
GO
