package com.disaster.dao;

import com.disaster.model.AuditLog;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AuditDAO {

    public List<AuditLog> getAllLogs() throws SQLException {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT al.*, u.UserName AS PerformedByName " +
                     "FROM Audit_Log al " +
                     "LEFT JOIN Users u ON al.Performedby = u.UserID " +
                     "ORDER BY al.Timestamp DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                AuditLog log = new AuditLog();
                log.setLogId(rs.getInt("LogID"));
                log.setTableAffected(rs.getString("TableAffected"));
                log.setAction(rs.getString("Action"));
                log.setOldValue(rs.getString("Oldvalue"));
                log.setNewValue(rs.getString("Newvalue"));
                log.setTimestamp(rs.getString("Timestamp"));
                log.setPerformedBy(rs.getInt("Performedby"));
                log.setPerformedByName(rs.getString("PerformedByName"));
                logs.add(log);
            }
        }

        return logs;
    }
}
