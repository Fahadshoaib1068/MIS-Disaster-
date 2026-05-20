USE disaster_mis;
GO

-- Baseline indexes for the schema.
IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'idx_report_event_severity'
      AND object_id = OBJECT_ID('dbo.Emergency_Report')
)
    CREATE INDEX idx_report_event_severity
        ON dbo.Emergency_Report(Eventid, SeverityLevel);

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'idx_transaction_date'
      AND object_id = OBJECT_ID('dbo.Financial_Transaction')
)
    CREATE INDEX idx_transaction_date
        ON dbo.Financial_Transaction(Transactiondate);

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'idx_transaction_type'
      AND object_id = OBJECT_ID('dbo.Financial_Transaction')
)
    CREATE INDEX idx_transaction_type
        ON dbo.Financial_Transaction(Transactiontype);

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'idx_allocation_status'
      AND object_id = OBJECT_ID('dbo.Resource_Allocation')
)
    CREATE INDEX idx_allocation_status
        ON dbo.Resource_Allocation(Status);

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'idx_team_availability'
      AND object_id = OBJECT_ID('dbo.Rescue_Team')
)
    CREATE INDEX idx_team_availability
        ON dbo.Rescue_Team(AvailabilityStatus);

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'idx_alert_recipient_read'
      AND object_id = OBJECT_ID('dbo.Alert')
)
    CREATE INDEX idx_alert_recipient_read
        ON dbo.Alert(Recipientuserid, Isread);

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'idx_auditlog_table'
      AND object_id = OBJECT_ID('dbo.Audit_Log')
)
    CREATE INDEX idx_auditlog_table
        ON dbo.Audit_Log(TableAffected);

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'idx_report_location'
      AND object_id = OBJECT_ID('dbo.Emergency_Report')
)
    CREATE INDEX idx_report_location
        ON dbo.Emergency_Report(Latitude, Longitude);

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'idx_disaster_type'
      AND object_id = OBJECT_ID('dbo.DisasterEventType')
)
    CREATE INDEX idx_disaster_type
        ON dbo.DisasterEventType(TypeID);

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'idx_resource_type'
      AND object_id = OBJECT_ID('dbo.Resources')
)
    CREATE INDEX idx_resource_type
        ON dbo.Resources(ResourceType);
GO

-- TEST 1: Emergency Reports by Event & Severity
SET STATISTICS TIME ON;
GO

IF EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'idx_report_event_severity'
      AND object_id = OBJECT_ID('dbo.Emergency_Report')
)
    DROP INDEX idx_report_event_severity ON dbo.Emergency_Report;
GO

SELECT ReportID, Address, SeverityLevel
FROM dbo.Emergency_Report
WHERE Eventid = 1
  AND SeverityLevel = 5;
GO

CREATE INDEX idx_report_event_severity
    ON dbo.Emergency_Report(Eventid, SeverityLevel);
GO

SELECT ReportID, Address, SeverityLevel
FROM dbo.Emergency_Report
WHERE Eventid = 1
  AND SeverityLevel = 5;
GO

SET STATISTICS TIME OFF;
GO

-- TEST 2: Financial Transactions by Type
SET STATISTICS TIME ON;
GO

IF EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'idx_transaction_type'
      AND object_id = OBJECT_ID('dbo.Financial_Transaction')
)
    DROP INDEX idx_transaction_type ON dbo.Financial_Transaction;
GO

SELECT Transactionid, Amount
FROM dbo.Financial_Transaction
WHERE Transactiontype = 'Donation';
GO

CREATE INDEX idx_transaction_type
    ON dbo.Financial_Transaction(Transactiontype);
GO

SELECT Transactionid, Amount
FROM dbo.Financial_Transaction
WHERE Transactiontype = 'Donation';
GO

SET STATISTICS TIME OFF;
GO

-- TEST 3: Financial Transactions by Date Range
SET STATISTICS TIME ON;
GO

IF EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'idx_transaction_date'
      AND object_id = OBJECT_ID('dbo.Financial_Transaction')
)
    DROP INDEX idx_transaction_date ON dbo.Financial_Transaction;
GO

SELECT Transactionid, Amount
FROM dbo.Financial_Transaction
WHERE Transactiondate BETWEEN '2025-07-01' AND '2025-09-30 23:59:59';
GO

CREATE INDEX idx_transaction_date
    ON dbo.Financial_Transaction(Transactiondate);
GO

SELECT Transactionid, Amount
FROM dbo.Financial_Transaction
WHERE Transactiondate BETWEEN '2025-07-01' AND '2025-09-30 23:59:59';
GO

SET STATISTICS TIME OFF;
GO

-- TEST 4: Rescue Team Availability
SET STATISTICS TIME ON;
GO

IF EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'idx_team_availability'
      AND object_id = OBJECT_ID('dbo.Rescue_Team')
)
    DROP INDEX idx_team_availability ON dbo.Rescue_Team;
GO

SELECT TeamID, Teamname
FROM dbo.Rescue_Team
WHERE AvailabilityStatus = 'Available';
GO

CREATE INDEX idx_team_availability
    ON dbo.Rescue_Team(AvailabilityStatus);
GO

SELECT TeamID, Teamname
FROM dbo.Rescue_Team
WHERE AvailabilityStatus = 'Available';
GO

SET STATISTICS TIME OFF;
GO

-- INDEX OVERHEAD ANALYSIS
SET STATISTICS TIME ON;
GO

INSERT INTO dbo.Emergency_Report
    (Latitude, Longitude, Address, SeverityLevel, Status, Description, Citizeninfo, Eventid)
VALUES
    (31.5000, 74.3000, 'Test Location, Lahore', 3, 'Pending', 'Test report for index overhead', 'Test Citizen', 1);
GO

UPDATE dbo.Emergency_Report
SET Status = 'Assigned'
WHERE ReportID = 1;
GO

SET STATISTICS TIME OFF;
GO

DELETE FROM dbo.Emergency_Report
WHERE Address = 'Test Location, Lahore'
  AND Citizeninfo = 'Test Citizen';
GO
