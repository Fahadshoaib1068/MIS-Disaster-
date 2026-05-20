package com.disaster.dao;

import com.disaster.model.FinancialTransaction;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FinancialDAO {
    private final ApprovalDAO approvalDAO = new ApprovalDAO();
    
    public List<FinancialTransaction> getAllTransactions() throws SQLException {
        List<FinancialTransaction> transactions = new ArrayList<>();
        String sql = "SELECT ft.*, u.UserName, de.EventName, ar.Status AS ApprovalStatus " +
                     "FROM Financial_Transaction ft " +
                     "LEFT JOIN Users u ON ft.Userid = u.UserID " +
                     "LEFT JOIN Disaster_Event de ON ft.Eventid = de.EventID " +
                     "LEFT JOIN Approval_Request ar ON ar.Referenceid = ft.Transactionid AND ar.Requesttype = 'Financial' " +
                     "ORDER BY ft.Transactiondate DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                FinancialTransaction t = new FinancialTransaction();
                t.setTransactionId(rs.getInt("Transactionid"));
                t.setTransactionType(rs.getString("Transactiontype"));
                t.setAmount(rs.getDouble("Amount"));
                t.setTransactionDate(rs.getString("Transactiondate"));
                t.setDescription(rs.getString("Description"));
                t.setReferenceNumber(rs.getString("Referencenumber"));
                t.setEventId(rs.getInt("Eventid"));
                t.setUserId(rs.getInt("Userid"));
                t.setDonorId(rs.getInt("Donorid"));
                t.setUserName(rs.getString("UserName"));
                t.setEventName(rs.getString("EventName"));
                t.setApprovalStatus(rs.getString("ApprovalStatus"));
                transactions.add(t);
            }
        }
        return transactions;
    }
    
    public double getTotalDonations() throws SQLException {
        String sql = "SELECT ISNULL(SUM(ft.Amount), 0) " +
                     "FROM Financial_Transaction ft " +
                     "LEFT JOIN Approval_Request ar ON ar.Referenceid = ft.Transactionid AND ar.Requesttype = 'Financial' " +
                     "WHERE ft.Transactiontype = 'Donation' AND (ar.Status IS NULL OR ar.Status = 'Approved')";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getDouble(1) : 0;
        }
    }
    
    public double getTotalExpenses() throws SQLException {
        String sql = "SELECT ISNULL(SUM(ft.Amount), 0) " +
                     "FROM Financial_Transaction ft " +
                     "LEFT JOIN Approval_Request ar ON ar.Referenceid = ft.Transactionid AND ar.Requesttype = 'Financial' " +
                     "WHERE ft.Transactiontype IN ('Expense', 'Procurement') AND (ar.Status IS NULL OR ar.Status = 'Approved')";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getDouble(1) : 0;
        }
    }
    
    public boolean addTransaction(FinancialTransaction transaction) throws SQLException {
        String sql = "INSERT INTO Financial_Transaction (Transactiontype, Amount, Description, Referencenumber, Eventid, Userid, Donorid) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, transaction.getTransactionType());
                pstmt.setDouble(2, transaction.getAmount());
                pstmt.setString(3, transaction.getDescription());
                pstmt.setString(4, transaction.getReferenceNumber());
                if (transaction.getEventId() > 0) {
                    pstmt.setInt(5, transaction.getEventId());
                } else {
                    pstmt.setNull(5, Types.INTEGER);
                }
                pstmt.setInt(6, transaction.getUserId());
                if (transaction.getDonorId() > 0) {
                    pstmt.setInt(7, transaction.getDonorId());
                } else {
                    pstmt.setNull(7, Types.INTEGER);
                }

                if (pstmt.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                int transactionId;
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (!rs.next()) {
                        conn.rollback();
                        throw new SQLException("Failed to create financial request.");
                    }
                    transactionId = rs.getInt(1);
                }

                approvalDAO.createApprovalRequest(conn, "Financial", transactionId, transaction.getUserId());
                insertAuditLog(conn, "Financial_Transaction", "INSERT", null, "Transactionid=" + transactionId + ",PendingApproval", transaction.getUserId());

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
