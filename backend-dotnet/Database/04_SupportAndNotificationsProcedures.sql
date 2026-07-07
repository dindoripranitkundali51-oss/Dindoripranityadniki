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
