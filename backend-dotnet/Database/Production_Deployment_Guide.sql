-- =====================================================================
-- DINDORI PRANIT YADNYIKI & VASTU SEVA
-- PRODUCTION DATABASE DEPLOYMENT GUIDE
-- =====================================================================
-- 
-- This script helps you deploy the database to a production MS SQL Server.
-- Follow the steps below in order.
--
-- PREREQUISITES:
-- 1. MS SQL Server installed on production server
-- 2. SQL Server Management Studio (SSMS) or sqlcmd CLI tool
-- 3. Appropriate permissions to create databases and stored procedures
--
-- =====================================================================

-- STEP 1: Create the Database (if it doesn't exist)
-- =====================================================================
USE master;
GO

IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'DindoriPranitDb')
BEGIN
    CREATE DATABASE DindoriPranitDb;
    PRINT 'Database DindoriPranitDb created successfully.';
END
ELSE
BEGIN
    PRINT 'Database DindoriPranitDb already exists.';
END
GO

-- STEP 2: Switch to the new database
-- =====================================================================
USE DindoriPranitDb;
GO

-- STEP 3: Create Tables
-- =====================================================================
-- Run the 01_CreateTables.sql script to create all tables
-- :r .\01_CreateTables.sql
PRINT 'Creating tables...';
-- (Copy contents of 01_CreateTables.sql here or run the file separately)

-- STEP 4: Create Stored Procedures
-- =====================================================================
-- Run the 02_StoredProcedures.sql script to create all stored procedures
-- :r .\02_StoredProcedures.sql
PRINT 'Creating stored procedures...';
-- (Copy contents of 02_StoredProcedures.sql here or run the file separately)

-- STEP 5: Create Support & Notification Procedures
-- =====================================================================
-- Run the 04_SupportAndNotificationsProcedures.sql script
-- :r .\04_SupportAndNotificationsProcedures.sql
PRINT 'Creating support and notification procedures...';
-- (Copy contents of 04_SupportAndNotificationsProcedures.sql here or run the file separately)

-- STEP 6: Seed Initial Data (Optional)
-- =====================================================================
-- Run the 03_SeedData.sql script to populate initial data
-- :r .\03_SeedData.sql
PRINT 'Seeding initial data...';
-- (Copy contents of 03_SeedData.sql here or run the file separately)

-- STEP 7: Create Database User for API (Security)
-- =====================================================================
-- Replace 'api_user' and 'StrongPassword123!' with your actual credentials
DECLARE @ApiUserName NVARCHAR(100) = 'api_user';
DECLARE @ApiPassword NVARCHAR(100) = 'StrongPassword123!';

IF NOT EXISTS (SELECT name FROM sys.server_principals WHERE name = @ApiUserName)
BEGIN
    CREATE LOGIN @ApiUserName WITH PASSWORD = @ApiPassword;
    PRINT 'SQL Login created successfully.';
END
ELSE
BEGIN
    PRINT 'SQL Login already exists.';
END
GO

USE DindoriPranitDb;
GO

IF NOT EXISTS (SELECT name FROM sys.database_principals WHERE name = 'api_user')
BEGIN
    CREATE USER api_user FOR LOGIN api_user;
    ALTER ROLE db_datareader ADD MEMBER api_user;
    ALTER ROLE db_datawriter ADD MEMBER api_user;
    ALTER ROLE db_owner ADD MEMBER api_user;
    PRINT 'Database user created and permissions granted.';
END
ELSE
BEGIN
    PRINT 'Database user already exists.';
END
GO

-- STEP 8: Verify Installation
-- =====================================================================
PRINT '=== DEPLOYMENT VERIFICATION ===';
PRINT 'Tables created:';
SELECT COUNT(*) AS TableCount FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE';
PRINT 'Stored procedures created:';
SELECT COUNT(*) AS ProcedureCount FROM INFORMATION_SCHEMA.ROUTINES WHERE ROUTINE_TYPE = 'PROCEDURE';
PRINT '=== DEPLOYMENT COMPLETE ===';
GO

-- =====================================================================
-- CONNECTION STRING FOR appsettings.Production.json:
-- =====================================================================
-- Server=YOUR_SERVER_ADDRESS;Database=DindoriPranitDb;User Id=api_user;Password=StrongPassword123!;TrustServerCertificate=True;MultipleActiveResultSets=true
--
-- Replace:
-- - YOUR_SERVER_ADDRESS with your actual SQL Server address (e.g., localhost, 192.168.1.100, or server.domain.com)
-- - StrongPassword123! with the actual password you set in STEP 7
-- =====================================================================
