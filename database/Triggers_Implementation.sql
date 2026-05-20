USE disaster_mis;
GO

-- TRIGGER 1
DROP TRIGGER IF EXISTS dbo.trg_reduce_inventory;
GO

CREATE TRIGGER dbo.trg_reduce_inventory
ON dbo.Resource_Allocation
AFTER UPDATE AS
BEGIN
UPDATE inv
SET inv.QuantityAvailable = inv.QuantityAvailable - i.Approvedqty
FROM dbo.Inventory inv
JOIN inserted i ON inv.Resourceid = i.Resourceid 
AND inv.Warehouseid = i.Warehouseid
JOIN deleted d ON d.Allocationid = i.Allocationid
WHERE i.Status = 'Approved' AND d.Status != 'Approved';
END;
GO


-- TRIGGER 2
DROP TRIGGER IF EXISTS dbo.trg_prevent_negative_inventory;
GO

CREATE TRIGGER dbo.trg_prevent_negative_inventory
ON dbo.Inventory
AFTER UPDATE
AS
BEGIN
IF EXISTS (
SELECT * FROM inserted 
WHERE QuantityAvailable < 0 )
BEGIN
PRINT 'Error: Inventory cannot be negative';
ROLLBACK TRANSACTION;
END
END;
GO


-- TRIGGER 3
DROP TRIGGER IF EXISTS dbo.trg_update_team_status;
GO

CREATE TRIGGER dbo.trg_update_team_status
ON dbo.Team_Assignment
AFTER INSERT
AS
BEGIN
UPDATE rt
SET rt.AvailabilityStatus = 'Assigned'
FROM dbo.Rescue_Team rt
JOIN inserted i ON rt.TeamID = i.Teamid;
END;
GO


-- TRIGGER 4
DROP TRIGGER IF EXISTS dbo.trg_financial_audit;
GO

CREATE TRIGGER dbo.trg_financial_audit
ON dbo.Financial_Transaction
AFTER INSERT
AS
BEGIN
INSERT INTO dbo.Audit_Log (TableAffected, Action, Newvalue, Performedby)
SELECT 'Financial_Transaction', 'INSERT', 'New transaction added', i.Userid
FROM inserted i;
END;
GO


-- TRIGGER 5
DROP TRIGGER IF EXISTS dbo.trg_low_stock_alert;
GO

CREATE TRIGGER dbo.trg_low_stock_alert
ON dbo.Inventory
AFTER UPDATE
AS
BEGIN
INSERT INTO dbo.Alert (Alerttype, Message, Recipientuserid)
SELECT 'LowStock',
       'Low stock: ' + r.ResourceName + ' at ' + w.WarehouseName +
       ' (available ' + CAST(i.QuantityAvailable AS VARCHAR(20)) +
       ', threshold ' + CAST(r.ThresholdLevel AS VARCHAR(20)) + ')',
       wm.UserID
FROM inserted i
JOIN dbo.Resources r ON i.Resourceid = r.ResourceID
JOIN dbo.Warehouse w ON i.Warehouseid = w.WarehouseID
JOIN dbo.Warehouse_Manager wm ON wm.AssignedWarehouseID = i.Warehouseid
WHERE i.QuantityAvailable < r.ThresholdLevel
UNION ALL
SELECT 'LowStock',
       'Low stock: ' + r.ResourceName + ' at ' + w.WarehouseName +
       ' (available ' + CAST(i.QuantityAvailable AS VARCHAR(20)) +
       ', threshold ' + CAST(r.ThresholdLevel AS VARCHAR(20)) + ')',
       ad.UserID
FROM inserted i
JOIN dbo.Resources r ON i.Resourceid = r.ResourceID
JOIN dbo.Warehouse w ON i.Warehouseid = w.WarehouseID
JOIN dbo.Administrator ad ON 1 = 1
WHERE i.QuantityAvailable < r.ThresholdLevel;
END;
GO
