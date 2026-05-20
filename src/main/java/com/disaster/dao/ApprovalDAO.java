package com.disaster.dao;

import com.disaster.model.ApprovalRequest;
import com.disaster.model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApprovalDAO {
    private static final Pattern RESTOCK_REFERENCE_PATTERN =
            Pattern.compile("^RSTK:W(\\d+):R(\\d+):Q(\\d+)$", Pattern.CASE_INSENSITIVE);
    
    public List<ApprovalRequest> getAllApprovals(User user) throws SQLException {
        return getApprovals(user, false);
    }

    public List<ApprovalRequest> getPendingApprovals(User user) throws SQLException {
        return getApprovals(user, true);
    }

    public int createApprovalRequest(Connection conn, String requestType, int referenceId, int requestedBy) throws SQLException {
        return createApprovalRequest(conn, requestType, referenceId, requestedBy, null);
    }

    public int createApprovalRequest(Connection conn, String requestType, int referenceId, int requestedBy, String comments) throws SQLException {
        String sql = "INSERT INTO Approval_Request (Requesttype, Status, Referenceid, Requestedby, Comments) VALUES (?, 'Pending', ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, requestType);
            pstmt.setInt(2, referenceId);
            pstmt.setInt(3, requestedBy);
            pstmt.setString(4, comments);
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Failed to create approval request.");
    }

    public boolean forwardResourceRequestToAdmin(int approvalId, int warehouseUserId, String comments) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ApprovalRequest approval = findApproval(conn, approvalId);
                if (approval == null
                        || !"ResourceAllocation".equalsIgnoreCase(approval.getRequestType())
                        || !"Pending".equalsIgnoreCase(approval.getStatus())) {
                    conn.rollback();
                    return false;
                }

                String reviewedComment = comments == null || comments.isBlank()
                        ? "Reviewed by warehouse manager and forwarded to administrator."
                        : comments.trim();

                try (PreparedStatement pstmt = conn.prepareStatement(
                        "UPDATE Approval_Request SET Status = 'Approved', Decisionat = GETDATE(), Approvedby = ?, Comments = ? WHERE Approvalid = ?")) {
                    pstmt.setInt(1, warehouseUserId);
                    pstmt.setString(2, reviewedComment);
                    pstmt.setInt(3, approvalId);
                    if (pstmt.executeUpdate() == 0) {
                        conn.rollback();
                        return false;
                    }
                }

                String adminComment = "Forwarded to administrator for final approval.";
                if (approval.getRequestedBy() != warehouseUserId) {
                    adminComment += " Original requester UserID=" + approval.getRequestedBy() + ".";
                }
                createApprovalRequest(conn, "ResourceAllocation", approval.getReferenceId(), warehouseUserId, adminComment);
                insertAuditLog(conn, "Approval_Request", "UPDATE", "WarehouseReviewPending", "ForwardedToAdmin", warehouseUserId);

                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public boolean updateApprovalStatus(int approvalId, String status, int approvedBy, String comments) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ApprovalRequest approval = findApproval(conn, approvalId);
                if (approval == null || !"Pending".equalsIgnoreCase(approval.getStatus())) {
                    conn.rollback();
                    return false;
                }

                String sql = "UPDATE Approval_Request SET Status = ?, Decisionat = GETDATE(), Approvedby = ?, Comments = ? WHERE Approvalid = ?";

                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, status);
                    pstmt.setInt(2, approvedBy);
                    pstmt.setString(3, comments);
                    pstmt.setInt(4, approvalId);
                    if (pstmt.executeUpdate() == 0) {
                        conn.rollback();
                        return false;
                    }
                }

                applyDecision(conn, approval, status, approvedBy);
                insertAuditLog(conn, "Approval_Request", "UPDATE", "Status=Pending", "Status=" + status, approvedBy);

                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private List<ApprovalRequest> getApprovals(User user, boolean pendingOnly) throws SQLException {
        List<ApprovalRequest> approvals = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT ar.*, ru.UserName AS RequestedByName, au.UserName AS ApprovedByName " +
                "FROM Approval_Request ar " +
                "LEFT JOIN Users ru ON ar.Requestedby = ru.UserID " +
                "LEFT JOIN Users au ON ar.Approvedby = au.UserID "
        );

        List<Object> params = new ArrayList<>();
        List<String> conditions = new ArrayList<>();

        if (pendingOnly) {
            conditions.add("ar.Status = 'Pending'");
        }

        if ("admin".equalsIgnoreCase(user.getRole())) {
            conditions.add("(" +
                    "ar.Requesttype <> 'ResourceAllocation' " +
                    "OR EXISTS (SELECT 1 FROM Warehouse_Manager wm WHERE wm.UserID = ar.Requestedby) " +
                    "OR EXISTS (SELECT 1 FROM Administrator ad WHERE ad.UserID = ar.Requestedby)" +
                    ")");
        } else {
            switch (user.getRole()) {
                case "warehouse":
                    conditions.add("(ar.Requesttype = 'ResourceAllocation' OR ar.Requestedby = ?)");
                    params.add(user.getUserId());
                    break;
                case "operator":
                    conditions.add("(ar.Requesttype = 'RescueDeployment' OR ar.Requestedby = ?)");
                    params.add(user.getUserId());
                    break;
                case "finance":
                    conditions.add("(ar.Requesttype = 'Financial' OR ar.Requestedby = ?)");
                    params.add(user.getUserId());
                    break;
                default:
                    conditions.add("ar.Requestedby = ?");
                    params.add(user.getUserId());
                    break;
            }
        }

        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }
        sql.append(" ORDER BY ar.Requestedat DESC");
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ApprovalRequest a = mapApproval(rs);
                    approvals.add(a);
                }
            }
        }
        return approvals;
    }

    private ApprovalRequest findApproval(Connection conn, int approvalId) throws SQLException {
        String sql = "SELECT * FROM Approval_Request WHERE Approvalid = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, approvalId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? mapApproval(rs) : null;
            }
        }
    }

    private ApprovalRequest mapApproval(ResultSet rs) throws SQLException {
        ApprovalRequest a = new ApprovalRequest();
        a.setApprovalId(rs.getInt("Approvalid"));
        a.setRequestType(rs.getString("Requesttype"));
        a.setRequestedAt(rs.getString("Requestedat"));
        a.setStatus(rs.getString("Status"));
        a.setDecisionAt(rs.getString("Decisionat"));
        a.setComments(rs.getString("Comments"));
        a.setReferenceId(rs.getInt("Referenceid"));
        a.setRequestedBy(rs.getInt("Requestedby"));
        a.setApprovedBy(rs.getInt("Approvedby"));
        try {
            a.setRequestedByName(rs.getString("RequestedByName"));
            a.setApprovedByName(rs.getString("ApprovedByName"));
        } catch (SQLException ignored) {
            // The lookup query does not always include joined names.
        }
        return a;
    }

    private void applyDecision(Connection conn, ApprovalRequest approval, String status, int approvedBy) throws SQLException {
        switch (approval.getRequestType()) {
            case "ResourceAllocation":
                applyResourceAllocationDecision(conn, approval.getReferenceId(), status);
                break;
            case "RescueDeployment":
                applyTeamDeploymentDecision(conn, approval.getReferenceId(), status);
                break;
            case "Financial":
                applyFinancialDecision(conn, approval.getReferenceId(), status, approvedBy);
                break;
            default:
                break;
        }
    }

    private void applyResourceAllocationDecision(Connection conn, int allocationId, String status) throws SQLException {
        String updateSql = "UPDATE Resource_Allocation " +
                "SET Status = ?, Approvedqty = CASE WHEN ? = 'Approved' THEN COALESCE(Approvedqty, Requestedqty) ELSE Approvedqty END " +
                "WHERE Allocationid = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            pstmt.setString(1, status);
            pstmt.setString(2, status);
            pstmt.setInt(3, allocationId);
            pstmt.executeUpdate();
        }

        if ("Approved".equalsIgnoreCase(status)) {
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "UPDATE inv " +
                    "SET inv.QuantityDispatched = inv.QuantityDispatched + ra.Approvedqty " +
                    "FROM Inventory inv " +
                    "JOIN Resource_Allocation ra ON inv.Warehouseid = ra.Warehouseid AND inv.Resourceid = ra.Resourceid " +
                    "WHERE ra.Allocationid = ? AND ra.Status = 'Approved'")) {
                pstmt.setInt(1, allocationId);
                pstmt.executeUpdate();
            }

            if (!hasTrigger(conn, "trg_reduce_inventory")) {
                String inventorySql =
                        "UPDATE inv SET inv.QuantityAvailable = inv.QuantityAvailable - ra.Approvedqty " +
                        "FROM Inventory inv " +
                        "JOIN Resource_Allocation ra ON inv.Warehouseid = ra.Warehouseid AND inv.Resourceid = ra.Resourceid " +
                        "WHERE ra.Allocationid = ? AND ra.Status = 'Approved'";
                try (PreparedStatement pstmt = conn.prepareStatement(inventorySql)) {
                    pstmt.setInt(1, allocationId);
                    pstmt.executeUpdate();
                }
            }
        }
    }

    private void applyTeamDeploymentDecision(Connection conn, int assignmentId, String status) throws SQLException {
        int teamId = 0;
        int reportId = 0;

        try (PreparedStatement pstmt = conn.prepareStatement("SELECT Teamid, Reportid FROM Team_Assignment WHERE AssignmentID = ?")) {
            pstmt.setInt(1, assignmentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    teamId = rs.getInt("Teamid");
                    reportId = rs.getInt("Reportid");
                }
            }
        }

        if (teamId == 0) {
            return;
        }

        if ("Approved".equalsIgnoreCase(status)) {
            if (!hasTrigger(conn, "trg_update_team_status")) {
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "UPDATE Rescue_Team SET AvailabilityStatus = 'Assigned' WHERE TeamID = ?")) {
                    pstmt.setInt(1, teamId);
                    pstmt.executeUpdate();
                }
            }

            try (PreparedStatement pstmt = conn.prepareStatement(
                    "UPDATE Emergency_Report SET Status = 'Assigned' WHERE ReportID = ?")) {
                pstmt.setInt(1, reportId);
                pstmt.executeUpdate();
            }
        } else if ("Rejected".equalsIgnoreCase(status)) {
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM Team_Assignment WHERE AssignmentID = ?")) {
                pstmt.setInt(1, assignmentId);
                pstmt.executeUpdate();
            }

            try (PreparedStatement pstmt = conn.prepareStatement(
                    "UPDATE Rescue_Team SET AvailabilityStatus = 'Available' WHERE TeamID = ?")) {
                pstmt.setInt(1, teamId);
                pstmt.executeUpdate();
            }
        }
    }

    private void applyFinancialDecision(Connection conn, int transactionId, String status, int approvedBy) throws SQLException {
        if (!"Approved".equalsIgnoreCase(status)) {
            return;
        }

        String sql = "SELECT Transactiontype, Referencenumber FROM Financial_Transaction WHERE Transactionid = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, transactionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    return;
                }

                String transactionType = rs.getString("Transactiontype");
                String referenceNumber = rs.getString("Referencenumber");
                if (!"Procurement".equalsIgnoreCase(transactionType)) {
                    return;
                }

                RestockTarget restockTarget = parseRestockReference(referenceNumber);
                if (restockTarget == null) {
                    return;
                }

                restockInventory(conn, restockTarget);
                insertAuditLog(
                        conn,
                        "Inventory",
                        "UPDATE",
                        null,
                        "Restocked Warehouse=" + restockTarget.warehouseId
                                + ",Resource=" + restockTarget.resourceId
                                + ",Qty=" + restockTarget.quantity
                                + ",Transaction=" + transactionId,
                        approvedBy);
            }
        }
    }

    private RestockTarget parseRestockReference(String referenceNumber) {
        if (referenceNumber == null) {
            return null;
        }

        Matcher matcher = RESTOCK_REFERENCE_PATTERN.matcher(referenceNumber.trim());
        if (!matcher.matches()) {
            return null;
        }

        return new RestockTarget(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3)));
    }

    private void restockInventory(Connection conn, RestockTarget restockTarget) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
                "UPDATE Inventory " +
                "SET QuantityAvailable = QuantityAvailable + ?, LastUpdated = GETDATE() " +
                "WHERE Warehouseid = ? AND Resourceid = ?")) {
            pstmt.setInt(1, restockTarget.quantity);
            pstmt.setInt(2, restockTarget.warehouseId);
            pstmt.setInt(3, restockTarget.resourceId);

            int updatedRows = pstmt.executeUpdate();
            if (updatedRows > 0) {
                return;
            }
        }

        try (PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO Inventory (Warehouseid, Resourceid, QuantityAvailable, QuantityDispatched, QuantityConsumed) " +
                "VALUES (?, ?, ?, 0, 0)")) {
            pstmt.setInt(1, restockTarget.warehouseId);
            pstmt.setInt(2, restockTarget.resourceId);
            pstmt.setInt(3, restockTarget.quantity);
            pstmt.executeUpdate();
        }
    }

    private boolean hasTrigger(Connection conn, String triggerName) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
                "SELECT COUNT(*) FROM sys.triggers WHERE name = ?")) {
            pstmt.setString(1, triggerName);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private void insertAuditLog(Connection conn, String tableAffected, String action, String oldValue, String newValue, int performedBy) throws SQLException {
        String sql = "INSERT INTO Audit_Log (TableAffected, Action, Oldvalue, Newvalue, Performedby) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tableAffected);
            pstmt.setString(2, action);
            pstmt.setString(3, oldValue);
            pstmt.setString(4, newValue);
            pstmt.setInt(5, performedBy);
            pstmt.executeUpdate();
        }
    }

    private static class RestockTarget {
        private final int warehouseId;
        private final int resourceId;
        private final int quantity;

        private RestockTarget(int warehouseId, int resourceId, int quantity) {
            this.warehouseId = warehouseId;
            this.resourceId = resourceId;
            this.quantity = quantity;
        }
    }
}
