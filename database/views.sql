USE disaster_mis;
GO

DROP VIEW IF EXISTS dbo.vw_UserRoles;
GO

CREATE VIEW dbo.vw_UserRoles AS
SELECT 
    u.UserID,
    u.UserName,
    u.Email,
    u.PasswordHash,
    u.PhoneNumber,
    u.IsActive,
    u.CreatedAt,
    CASE 
        WHEN a.UserID IS NOT NULL THEN 'admin'
        WHEN eo.UserID IS NOT NULL THEN 'operator'
        WHEN fo.UserID IS NOT NULL THEN 'field'
        WHEN wm.UserID IS NOT NULL THEN 'warehouse'
        WHEN fo2.UserID IS NOT NULL THEN 'finance'
        ELSE 'user'
    END as Role
FROM dbo.Users u
LEFT JOIN dbo.Administrator a ON u.UserID = a.UserID
LEFT JOIN dbo.Emergency_Operator eo ON u.UserID = eo.UserID
LEFT JOIN dbo.Field_Officer fo ON u.UserID = fo.UserID
LEFT JOIN dbo.Warehouse_Manager wm ON u.UserID = wm.UserID
LEFT JOIN dbo.Finance_Officer fo2 ON u.UserID = fo2.UserID;
GO

-- Create view for dashboard statistics
DROP VIEW IF EXISTS dbo.vw_DashboardStats;
GO

CREATE VIEW dbo.vw_DashboardStats AS
SELECT 
    (SELECT COUNT(*) FROM dbo.Disaster_Event WHERE Status = 'Active') as ActiveEvents,
    (SELECT COUNT(*) FROM dbo.Emergency_Report WHERE Status IN ('Pending', 'Assigned')) as OpenReports,
    (SELECT COUNT(*) FROM dbo.Rescue_Team WHERE AvailabilityStatus IN ('Assigned', 'Busy')) as DeployedTeams,
    (SELECT COUNT(*) FROM dbo.Approval_Request WHERE Status = 'Pending') as PendingApprovals,
    (SELECT COUNT(*) FROM dbo.Patient) as PatientsAdmitted,
    (SELECT COUNT(*)
     FROM dbo.Inventory i
     JOIN dbo.Resources r ON i.Resourceid = r.ResourceID
     WHERE i.QuantityAvailable < r.ThresholdLevel) as LowStockAlerts;
GO

-- Create view for financial summary
DROP VIEW IF EXISTS dbo.vw_FinancialSummary;
GO

CREATE VIEW dbo.vw_FinancialSummary AS
SELECT 
    'Donations' as Type,
    ISNULL(SUM(Amount), 0) as Total
FROM dbo.Financial_Transaction
WHERE Transactiontype = 'Donation'
UNION ALL
SELECT
    'Expenses' as Type,
    ISNULL(SUM(Amount), 0) as Total
FROM dbo.Financial_Transaction
WHERE Transactiontype IN ('Expense', 'Procurement');
GO
