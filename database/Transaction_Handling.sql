USE disaster_mis;
GO

SET XACT_ABORT ON;
GO

-- ============================================================
-- TRANSACTION 1: Approve Resource Allocation & Update Inventory
-- ============================================================
BEGIN TRANSACTION;
BEGIN TRY
    UPDATE dbo.Resource_Allocation
    SET Status = 'Approved',
        Approvedqty = 100
    WHERE Allocationid = 4;

    UPDATE dbo.Inventory
    SET QuantityAvailable = QuantityAvailable - 100
    WHERE Warehouseid = 3
      AND Resourceid = 4
      AND QuantityAvailable >= 100; -- Guard: prevent negative inventory

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;
    THROW; -- Re-raise error so it is visible
END CATCH;
GO


-- ============================================================
-- TRANSACTION 2: Assign Team & Update Availability Status
-- ============================================================
BEGIN TRANSACTION;
BEGIN TRY
    INSERT INTO dbo.Team_Assignment (Teamid, Reportid, Authorizedby, Status)
    VALUES (3, 1, 5, 'Active');

    UPDATE dbo.Rescue_Team
    SET AvailabilityStatus = 'Busy'
    WHERE TeamID = 3;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO


-- ============================================================
-- TRANSACTION 3: Record Financial Expense
-- ============================================================
BEGIN TRANSACTION;
BEGIN TRY
    INSERT INTO dbo.Financial_Transaction (Transactiontype, Amount, Eventid, Userid)
    VALUES ('Expense', 50000, 1, 9);

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO


-- ============================================================
-- TRANSACTION 4: Admit Patient & Update Hospital Bed Count
-- ============================================================
BEGIN TRANSACTION;
BEGIN TRY
    INSERT INTO dbo.Patient (Name, Age, Gender, Condition, Admissiontime, Hospitalid, Reportid)
    VALUES ('Test Patient', 30, 'Male', 'Critical', GETDATE(), 1, 1);

    UPDATE dbo.Hospital
    SET Availablebeds = Availablebeds - 1
    WHERE HospitalID = 1
      AND Availablebeds > 0; -- Guard: prevent negative bed count

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO


-- ============================================================
-- TRANSACTION 5: Approve Approval Request
-- ============================================================
BEGIN TRANSACTION;
BEGIN TRY
    UPDATE dbo.Approval_Request
    SET Status = 'Approved', Decisionat = GETDATE()
    WHERE Approvalid = 1;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
