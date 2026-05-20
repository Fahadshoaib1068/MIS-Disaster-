package com.disaster.dao;

import com.disaster.model.DisasterEvent;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventDAO {
    
    public List<DisasterEvent> getAllEvents() throws SQLException {
        List<DisasterEvent> events = new ArrayList<>();
        String sql = "SELECT de.*, dt.TypeName AS DisasterType " +
                     "FROM Disaster_Event de " +
                     "LEFT JOIN DisasterEventType det ON de.EventID = det.EventID " +
                     "LEFT JOIN Disaster_Type dt ON det.TypeID = dt.TypeID " +
                     "ORDER BY de.StartDate DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                events.add(mapEvent(rs));
            }
        }
        return events;
    }
    
    public List<DisasterEvent> getActiveEvents() throws SQLException {
        List<DisasterEvent> events = new ArrayList<>();
        String sql = "SELECT de.*, dt.TypeName AS DisasterType " +
                     "FROM Disaster_Event de " +
                     "LEFT JOIN DisasterEventType det ON de.EventID = det.EventID " +
                     "LEFT JOIN Disaster_Type dt ON det.TypeID = dt.TypeID " +
                     "WHERE de.Status = 'Active' " +
                     "ORDER BY de.SeverityLevel DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                events.add(mapEvent(rs));
            }
        }
        return events;
    }
    
    public boolean addEvent(DisasterEvent event) throws SQLException {
        String eventSql = "INSERT INTO Disaster_Event (EventName, AffectedRegion, StartDate, EndDate, SeverityLevel, Status, BudgetAllocated) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String findTypeSql = "SELECT TypeID FROM Disaster_Type WHERE TypeName = ?";
        String insertTypeSql = "INSERT INTO Disaster_Type (TypeName) VALUES (?)";
        String linkTypeSql = "INSERT INTO DisasterEventType (TypeID, EventID) VALUES (?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement eventStmt = conn.prepareStatement(eventSql, Statement.RETURN_GENERATED_KEYS)) {
                eventStmt.setString(1, event.getEventName());
                eventStmt.setString(2, event.getAffectedRegion());
                eventStmt.setString(3, event.getStartDate());
                eventStmt.setString(4, blankToNull(event.getEndDate()));
                eventStmt.setInt(5, event.getSeverityLevel());
                eventStmt.setString(6, event.getStatus());
                eventStmt.setDouble(7, event.getBudgetAllocated());

                if (eventStmt.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                int eventId;
                try (ResultSet rs = eventStmt.getGeneratedKeys()) {
                    if (!rs.next()) {
                        conn.rollback();
                        throw new SQLException("Failed to create disaster event.");
                    }
                    eventId = rs.getInt(1);
                }

                Integer typeId = null;
                String disasterType = blankToNull(event.getDisasterType());
                if (disasterType != null) {
                    try (PreparedStatement findTypeStmt = conn.prepareStatement(findTypeSql)) {
                        findTypeStmt.setString(1, disasterType);
                        try (ResultSet rs = findTypeStmt.executeQuery()) {
                            if (rs.next()) {
                                typeId = rs.getInt(1);
                            }
                        }
                    }

                    if (typeId == null) {
                        try (PreparedStatement insertTypeStmt = conn.prepareStatement(insertTypeSql, Statement.RETURN_GENERATED_KEYS)) {
                            insertTypeStmt.setString(1, disasterType);
                            if (insertTypeStmt.executeUpdate() == 0) {
                                conn.rollback();
                                return false;
                            }
                            try (ResultSet rs = insertTypeStmt.getGeneratedKeys()) {
                                if (!rs.next()) {
                                    conn.rollback();
                                    throw new SQLException("Failed to create disaster type.");
                                }
                                typeId = rs.getInt(1);
                            }
                        }
                    }

                    try (PreparedStatement linkTypeStmt = conn.prepareStatement(linkTypeSql)) {
                        linkTypeStmt.setInt(1, typeId);
                        linkTypeStmt.setInt(2, eventId);
                        linkTypeStmt.executeUpdate();
                    }
                }

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

    private DisasterEvent mapEvent(ResultSet rs) throws SQLException {
        DisasterEvent event = new DisasterEvent();
        event.setEventId(rs.getInt("EventID"));
        event.setEventName(rs.getString("EventName"));
        event.setDisasterType(rs.getString("DisasterType"));
        event.setAffectedRegion(rs.getString("AffectedRegion"));
        event.setStartDate(rs.getString("StartDate"));
        event.setEndDate(rs.getString("EndDate"));
        event.setSeverityLevel(rs.getInt("SeverityLevel"));
        event.setStatus(rs.getString("Status"));
        event.setBudgetAllocated(rs.getDouble("BudgetAllocated"));
        return event;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
