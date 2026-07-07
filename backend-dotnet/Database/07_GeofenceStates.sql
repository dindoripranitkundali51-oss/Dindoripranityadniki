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
