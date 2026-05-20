package com.disaster.dao;

import com.disaster.model.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ResourceDAO {
    private final ApprovalDAO approvalDAO = new ApprovalDAO();
    
    public List<InventoryItem> getInventory() throws SQLException {
        List<InventoryItem> inventory = new ArrayList<>();
        String sql = "SELECT i.*, r.ResourceName, r.ResourceType, r.Unit, r.ThresholdLevel, " +
                     "w.WarehouseName, w.City, w.Capacity " +
                     "FROM Inventory i " +
                     "JOIN Resources r ON i.Resourceid = r.ResourceID " +
                     "JOIN Warehouse w ON i.Warehouseid = w.WarehouseID";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                InventoryItem item = new InventoryItem();
                item.setWarehouseId(rs.getInt("Warehouseid"));
                item.setWarehouseName(rs.getString("WarehouseName"));
                item.setCity(rs.getString("City"));
                item.setWarehouseCapacity(rs.getInt("Capacity"));
                item.setResourceId(rs.getInt("Resourceid"));
                item.setResourceName(rs.getString("ResourceName"));
                item.setResourceType(rs.getString("ResourceType"));
                item.setUnit(rs.getString("Unit"));
                item.setQuantityAvailable(rs.getInt("QuantityAvailable"));
                item.setQuantityDispatched(rs.getInt("QuantityDispatched"));
                item.setQuantityConsumed(rs.getInt("QuantityConsumed"));
                item.setThresholdLevel(rs.getInt("ThresholdLevel"));
                item.setLastUpdated(rs.getString("LastUpdated"));
                inventory.add(item);
            }
        }
        return inventory;
    }
    
    public List<InventoryItem> getLowStockItems() throws SQLException {
        List<InventoryItem> lowStock = new ArrayList<>();
        String sql = "SELECT i.*, r.ResourceName, r.ResourceType, r.Unit, r.ThresholdLevel, " +
                     "w.WarehouseName, w.City, w.Capacity " +
                     "FROM Inventory i " +
                     "JOIN Resources r ON i.Resourceid = r.ResourceID " +
                     "JOIN Warehouse w ON i.Warehouseid = w.WarehouseID " +
                     "WHERE i.QuantityAvailable < r.ThresholdLevel";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                InventoryItem item = new InventoryItem();
                item.setWarehouseId(rs.getInt("Warehouseid"));
                item.setWarehouseName(rs.getString("WarehouseName"));
                item.setCity(rs.getString("City"));
                item.setWarehouseCapacity(rs.getInt("Capacity"));
                item.setResourceId(rs.getInt("Resourceid"));
                item.setResourceName(rs.getString("ResourceName"));
                item.setResourceType(rs.getString("ResourceType"));
                item.setUnit(rs.getString("Unit"));
                item.setQuantityAvailable(rs.getInt("QuantityAvailable"));
                item.setQuantityDispatched(rs.getInt("QuantityDispatched"));
                item.setQuantityConsumed(rs.getInt("QuantityConsumed"));
                item.setThresholdLevel(rs.getInt("ThresholdLevel"));
                lowStock.add(item);
            }
        }
        return lowStock;
    }
    
    public boolean requestAllocation(AllocationRequest request) throws SQLException {
        String sql = "INSERT INTO Resource_Allocation (Resourceid, Warehouseid, Eventid, " +
                     "Requestedby, Requestedqty, Status, Purpose) VALUES (?, ?, ?, ?, ?, 'Pending', ?)";
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, request.getResourceId());
                pstmt.setInt(2, request.getWarehouseId());
                pstmt.setInt(3, request.getEventId());
                pstmt.setInt(4, request.getRequestedBy());
                pstmt.setInt(5, request.getRequestedQty());
                pstmt.setString(6, request.getPurpose());

                if (pstmt.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                int allocationId;
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (!rs.next()) {
                        conn.rollback();
                        throw new SQLException("Failed to create resource allocation request.");
                    }
                    allocationId = rs.getInt(1);
                }

                approvalDAO.createApprovalRequest(conn, "ResourceAllocation", allocationId, request.getRequestedBy());
                insertAuditLog(conn, "Resource_Allocation", "INSERT", null, "Allocationid=" + allocationId + ",Status=Pending", request.getRequestedBy());

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

    public boolean requestProcurement(ProcurementRequest request) throws SQLException {
        if (request == null
                || request.getWarehouseId() <= 0
                || request.getResourceId() <= 0
                || request.getRequestedQty() <= 0
                || request.getRequestedBy() <= 0
                || request.getReason() == null
                || request.getReason().isBlank()) {
            return false;
        }

        String inventorySql = "SELECT i.Warehouseid, i.Resourceid, i.QuantityAvailable, r.ThresholdLevel, " +
                              "r.ResourceName, r.Unit, w.WarehouseName " +
                              "FROM Inventory i " +
                              "JOIN Resources r ON i.Resourceid = r.ResourceID " +
                              "JOIN Warehouse w ON i.Warehouseid = w.WarehouseID " +
                              "WHERE i.Warehouseid = ? AND i.Resourceid = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(inventorySql)) {
                pstmt.setInt(1, request.getWarehouseId());
                pstmt.setInt(2, request.getResourceId());

                String resourceName;
                String unit;
                String warehouseName;
                int availableQty;
                int thresholdLevel;

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }

                    availableQty = rs.getInt("QuantityAvailable");
                    thresholdLevel = rs.getInt("ThresholdLevel");
                    resourceName = rs.getString("ResourceName");
                    unit = rs.getString("Unit");
                    warehouseName = rs.getString("WarehouseName");
                }

                if (availableQty >= thresholdLevel) {
                    conn.rollback();
                    return false;
                }

                List<Integer> financeOfficerIds = getFinanceOfficerIds(conn);
                if (financeOfficerIds.isEmpty()) {
                    conn.rollback();
                    throw new SQLException("No finance officers are configured to receive procurement requests.");
                }

                String message = buildProcurementAlertMessage(request, resourceName, unit, warehouseName, availableQty, thresholdLevel);
                for (Integer financeOfficerId : financeOfficerIds) {
                    insertAlert(conn, "Escalation", message, financeOfficerId);
                }

                insertAuditLog(
                        conn,
                        "Alert",
                        "INSERT",
                        null,
                        "ProcurementRequest:Warehouse=" + request.getWarehouseId()
                                + ",Resource=" + request.getResourceId()
                                + ",Qty=" + request.getRequestedQty(),
                        request.getRequestedBy());

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
    
    public List<AllocationRequest> getAllocations() throws SQLException {
        List<AllocationRequest> allocations = new ArrayList<>();
        String sql = "SELECT ra.*, r.ResourceName, w.WarehouseName, de.EventName, u.UserName as RequestedByName, " +
                     "latest.Approvalid AS ApprovalID, latest.Status AS ApprovalStatus, latest.Requestedby AS ApprovalRequestedBy, " +
                     "au.UserName AS ApprovalRequestedByName, latest.Comments AS ApprovalComments, approval_counts.ApprovalCount " +
                     "FROM Resource_Allocation ra " +
                     "JOIN Resources r ON ra.Resourceid = r.ResourceID " +
                     "JOIN Warehouse w ON ra.Warehouseid = w.WarehouseID " +
                     "JOIN Disaster_Event de ON ra.Eventid = de.EventID " +
                     "JOIN Users u ON ra.Requestedby = u.UserID " +
                     "OUTER APPLY (" +
                     "  SELECT TOP 1 ar.Approvalid, ar.Status, ar.Requestedby, ar.Comments " +
                     "  FROM Approval_Request ar " +
                     "  WHERE ar.Referenceid = ra.Allocationid AND ar.Requesttype = 'ResourceAllocation' " +
                     "  ORDER BY ar.Approvalid DESC" +
                     ") latest " +
                     "OUTER APPLY (" +
                     "  SELECT COUNT(*) AS ApprovalCount " +
                     "  FROM Approval_Request ar2 " +
                     "  WHERE ar2.Referenceid = ra.Allocationid AND ar2.Requesttype = 'ResourceAllocation'" +
                     ") approval_counts " +
                     "LEFT JOIN Users au ON latest.Requestedby = au.UserID " +
                     "ORDER BY ra.Allocationdate DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                AllocationRequest alloc = new AllocationRequest();
                alloc.setAllocationId(rs.getInt("Allocationid"));
                alloc.setResourceId(rs.getInt("Resourceid"));
                alloc.setResourceName(rs.getString("ResourceName"));
                alloc.setWarehouseId(rs.getInt("Warehouseid"));
                alloc.setWarehouseName(rs.getString("WarehouseName"));
                alloc.setEventId(rs.getInt("Eventid"));
                alloc.setEventName(rs.getString("EventName"));
                alloc.setRequestedBy(rs.getInt("Requestedby"));
                alloc.setRequestedByName(rs.getString("RequestedByName"));
                alloc.setRequestedQty(rs.getInt("Requestedqty"));
                Integer approvedQty = rs.getObject("Approvedqty") != null ? rs.getInt("Approvedqty") : null;
                alloc.setApprovedQty(approvedQty);
                alloc.setAllocationDate(rs.getString("Allocationdate"));
                alloc.setStatus(rs.getString("Status"));
                alloc.setPurpose(rs.getString("Purpose"));
                Object approvalId = rs.getObject("ApprovalID");
                alloc.setApprovalId(approvalId == null ? null : rs.getInt("ApprovalID"));
                alloc.setApprovalStatus(rs.getString("ApprovalStatus"));
                Object approvalRequestedBy = rs.getObject("ApprovalRequestedBy");
                alloc.setApprovalRequestedBy(approvalRequestedBy == null ? null : rs.getInt("ApprovalRequestedBy"));
                alloc.setApprovalRequestedByName(rs.getString("ApprovalRequestedByName"));
                alloc.setApprovalComments(rs.getString("ApprovalComments"));
                alloc.setApprovalCount(rs.getInt("ApprovalCount"));
                allocations.add(alloc);
            }
        }
        return allocations;
    }

    public boolean forwardAllocationToAdmin(int approvalId, int warehouseUserId, String comments) throws SQLException {
        return approvalDAO.forwardResourceRequestToAdmin(approvalId, warehouseUserId, comments);
    }

    private List<Integer> getFinanceOfficerIds(Connection conn) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT UserID FROM Finance_Officer");
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                ids.add(rs.getInt("UserID"));
            }
        }
        return ids;
    }

    private void insertAlert(Connection conn, String alertType, String message, int recipientUserId) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO Alert (Alerttype, Message, Recipientuserid) VALUES (?, ?, ?)")) {
            pstmt.setString(1, alertType);
            pstmt.setString(2, message);
            pstmt.setInt(3, recipientUserId);
            pstmt.executeUpdate();
        }
    }

    private String buildProcurementAlertMessage(
            ProcurementRequest request,
            String resourceName,
            String unit,
            String warehouseName,
            int availableQty,
            int thresholdLevel) {
        StringBuilder message = new StringBuilder();
        message.append("Procurement requested for ")
               .append(resourceName)
               .append(" at ")
               .append(warehouseName)
               .append(". Restock ")
               .append(request.getRequestedQty())
               .append(' ')
               .append(unit)
               .append(" (available ")
               .append(availableQty)
               .append(", threshold ")
               .append(thresholdLevel)
               .append(").");

        if (request.getReason() != null && !request.getReason().isBlank()) {
            String note = truncate(request.getReason().trim(), 60);
            if (note.endsWith(".")) {
                note = note.substring(0, note.length() - 1);
            }
            message.append(" Note: ").append(note).append('.');
        }

        message.append(" [PROC_META|W=")
               .append(request.getWarehouseId())
               .append("|R=")
               .append(request.getResourceId())
               .append("|Q=")
               .append(request.getRequestedQty())
               .append("]");
        return message.toString();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
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
}
