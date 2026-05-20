package com.disaster.dao;

import com.disaster.model.Alert;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AlertDAO {
    
    public List<Alert> getAllAlerts() throws SQLException {
        return getAlertsForRecipient(null);
    }

    public List<Alert> getAlertsForRecipient(Integer userId) throws SQLException {
        List<Alert> alerts = new ArrayList<>();
        String sql = "SELECT * FROM Alert " +
                     (userId != null ? "WHERE Recipientuserid = ? " : "") +
                     "ORDER BY Createdat DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (userId != null) {
                pstmt.setInt(1, userId);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Alert alert = new Alert();
                    alert.setAlertId(rs.getInt("Alertid"));
                    alert.setAlertType(rs.getString("Alerttype"));
                    alert.setMessage(rs.getString("Message"));
                    alert.setCreatedAt(rs.getString("Createdat"));
                    alert.setRead(rs.getBoolean("Isread"));
                    alert.setRecipientUserId(rs.getInt("Recipientuserid"));
                    alerts.add(alert);
                }
            }
        }
        return alerts;
    }
    
    public List<Alert> getUnreadAlerts(int userId) throws SQLException {
        List<Alert> alerts = new ArrayList<>();
        String sql = "SELECT * FROM Alert WHERE Recipientuserid = ? AND Isread = 0 ORDER BY Createdat DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Alert alert = new Alert();
                alert.setAlertId(rs.getInt("Alertid"));
                alert.setAlertType(rs.getString("Alerttype"));
                alert.setMessage(rs.getString("Message"));
                alert.setCreatedAt(rs.getString("Createdat"));
                alert.setRead(rs.getBoolean("Isread"));
                alert.setRecipientUserId(rs.getInt("Recipientuserid"));
                alerts.add(alert);
            }
        }
        return alerts;
    }
    
    public boolean markAsRead(int alertId, Integer userId) throws SQLException {
        String sql = "UPDATE Alert SET Isread = 1 WHERE Alertid = ?" +
                     (userId != null ? " AND Recipientuserid = ?" : "");
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, alertId);
            if (userId != null) {
                pstmt.setInt(2, userId);
            }
            return pstmt.executeUpdate() > 0;
        }
    }
}
