package com.disaster.dao;

import com.disaster.model.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReportDAO {
    
    public boolean addReport(EmergencyReport report) throws SQLException {
        String sql = "INSERT INTO Emergency_Report (Latitude, Longitude, Address, SeverityLevel, " +
                     "Status, Description, Citizeninfo, Eventid) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setDouble(1, report.getLatitude());
            pstmt.setDouble(2, report.getLongitude());
            pstmt.setString(3, report.getAddress());
            pstmt.setInt(4, report.getSeverityLevel());
            pstmt.setString(5, "Pending");
            pstmt.setString(6, report.getDescription());
            pstmt.setString(7, report.getCitizenInfo());
            pstmt.setInt(8, report.getEventId());
            
            return pstmt.executeUpdate() > 0;
        }
    }
    
    public List<EmergencyReport> getAllReports() throws SQLException {
        List<EmergencyReport> reports = new ArrayList<>();
        String sql = "SELECT er.*, de.EventName, dt.TypeName as DisasterType " +
                     "FROM Emergency_Report er " +
                     "LEFT JOIN Disaster_Event de ON er.Eventid = de.EventID " +
                     "LEFT JOIN DisasterEventType det ON de.EventID = det.EventID " +
                     "LEFT JOIN Disaster_Type dt ON det.TypeID = dt.TypeID " +
                     "ORDER BY er.Reporttime DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                EmergencyReport report = new EmergencyReport();
                report.setReportId(rs.getInt("ReportID"));
                report.setLatitude(rs.getDouble("Latitude"));
                report.setLongitude(rs.getDouble("Longitude"));
                report.setAddress(rs.getString("Address"));
                report.setSeverityLevel(rs.getInt("SeverityLevel"));
                report.setReportTime(rs.getString("Reporttime"));
                report.setStatus(rs.getString("Status"));
                report.setDescription(rs.getString("Description"));
                report.setCitizenInfo(rs.getString("Citizeninfo"));
                report.setEventId(rs.getInt("Eventid"));
                report.setEventName(rs.getString("EventName"));
                report.setDisasterType(rs.getString("DisasterType"));
                reports.add(report);
            }
        }
        return reports;
    }
    
    public List<EmergencyReport> getActiveReports() throws SQLException {
        List<EmergencyReport> reports = new ArrayList<>();
        String sql = "SELECT er.*, de.EventName, dt.TypeName as DisasterType " +
                     "FROM Emergency_Report er " +
                     "LEFT JOIN Disaster_Event de ON er.Eventid = de.EventID " +
                     "LEFT JOIN DisasterEventType det ON de.EventID = det.EventID " +
                     "LEFT JOIN Disaster_Type dt ON det.TypeID = dt.TypeID " +
                     "WHERE er.Status IN ('Pending', 'Active', 'Assigned') " +
                     "ORDER BY er.SeverityLevel DESC, er.Reporttime ASC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                EmergencyReport report = new EmergencyReport();
                report.setReportId(rs.getInt("ReportID"));
                report.setLatitude(rs.getDouble("Latitude"));
                report.setLongitude(rs.getDouble("Longitude"));
                report.setAddress(rs.getString("Address"));
                report.setSeverityLevel(rs.getInt("SeverityLevel"));
                report.setReportTime(rs.getString("Reporttime"));
                report.setStatus(rs.getString("Status"));
                report.setDescription(rs.getString("Description"));
                report.setCitizenInfo(rs.getString("Citizeninfo"));
                report.setEventId(rs.getInt("Eventid"));
                report.setEventName(rs.getString("EventName"));
                report.setDisasterType(rs.getString("DisasterType"));
                reports.add(report);
            }
        }
        return reports;
    }
    
    public boolean updateReportStatus(int reportId, String status) throws SQLException {
        String sql = "UPDATE Emergency_Report SET Status = ? WHERE ReportID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, status);
            pstmt.setInt(2, reportId);
            return pstmt.executeUpdate() > 0;
        }
    }
    
    public EmergencyReport getReportById(int reportId) throws SQLException {
        String sql = "SELECT er.*, de.EventName, dt.TypeName as DisasterType " +
                     "FROM Emergency_Report er " +
                     "LEFT JOIN Disaster_Event de ON er.Eventid = de.EventID " +
                     "LEFT JOIN DisasterEventType det ON de.EventID = det.EventID " +
                     "LEFT JOIN Disaster_Type dt ON det.TypeID = dt.TypeID " +
                     "WHERE er.ReportID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, reportId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    EmergencyReport report = new EmergencyReport();
                    report.setReportId(rs.getInt("ReportID"));
                    report.setLatitude(rs.getDouble("Latitude"));
                    report.setLongitude(rs.getDouble("Longitude"));
                    report.setAddress(rs.getString("Address"));
                    report.setSeverityLevel(rs.getInt("SeverityLevel"));
                    report.setReportTime(rs.getString("Reporttime"));
                    report.setStatus(rs.getString("Status"));
                    report.setDescription(rs.getString("Description"));
                    report.setCitizenInfo(rs.getString("Citizeninfo"));
                    report.setEventId(rs.getInt("Eventid"));
                    report.setEventName(rs.getString("EventName"));
                    report.setDisasterType(rs.getString("DisasterType"));
                    return report;
                }
            }
        }
        return null;
    }
}