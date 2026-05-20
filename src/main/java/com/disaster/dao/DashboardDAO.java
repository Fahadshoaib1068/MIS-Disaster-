package com.disaster.dao;

import com.disaster.model.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DashboardDAO {
    
    public DashboardStats getDashboardStats() throws SQLException {
        DashboardStats stats = new DashboardStats();
        
        String sqlActiveEvents = "SELECT COUNT(*) FROM Disaster_Event WHERE Status = 'Active'";
        String sqlOpenReports = "SELECT COUNT(*) FROM Emergency_Report WHERE Status IN ('Pending', 'Active', 'Assigned')";
        String sqlDeployedTeams = "SELECT COUNT(*) FROM Rescue_Team WHERE AvailabilityStatus IN ('Assigned', 'Busy')";
        String sqlPendingApprovals = "SELECT COUNT(*) FROM Approval_Request WHERE Status = 'Pending'";
        String sqlPatients = "SELECT COUNT(*) FROM Patient";
        String sqlLowStock = "SELECT COUNT(*) FROM Inventory i JOIN Resources r ON i.Resourceid = r.ResourceID WHERE i.QuantityAvailable < r.ThresholdLevel";
        String sqlTotalDonations = "SELECT ISNULL(SUM(Amount), 0) FROM Financial_Transaction WHERE Transactiontype = 'Donation'";
        String sqlTotalExpenses = "SELECT ISNULL(SUM(Amount), 0) FROM Financial_Transaction WHERE Transactiontype IN ('Expense', 'Procurement')";
        String sqlAvailableTeams = "SELECT COUNT(*) FROM Rescue_Team WHERE AvailabilityStatus = 'Available'";
        String sqlBusyTeams = "SELECT COUNT(*) FROM Rescue_Team WHERE AvailabilityStatus = 'Busy'";
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery(sqlActiveEvents);
                if (rs.next()) stats.setActiveEvents(rs.getInt(1));
                
                rs = stmt.executeQuery(sqlOpenReports);
                if (rs.next()) stats.setOpenReports(rs.getInt(1));
                
                rs = stmt.executeQuery(sqlDeployedTeams);
                if (rs.next()) stats.setDeployedTeams(rs.getInt(1));
                
                rs = stmt.executeQuery(sqlPendingApprovals);
                if (rs.next()) stats.setPendingApprovals(rs.getInt(1));
                
                rs = stmt.executeQuery(sqlPatients);
                if (rs.next()) stats.setPatientsAdmitted(rs.getInt(1));
                
                rs = stmt.executeQuery(sqlLowStock);
                if (rs.next()) stats.setLowStockAlerts(rs.getInt(1));
                
                rs = stmt.executeQuery(sqlTotalDonations);
                if (rs.next()) stats.setTotalDonations(rs.getDouble(1));
                
                rs = stmt.executeQuery(sqlTotalExpenses);
                if (rs.next()) stats.setTotalExpenses(rs.getDouble(1));
                
                rs = stmt.executeQuery(sqlAvailableTeams);
                if (rs.next()) stats.setAvailableTeams(rs.getInt(1));
                
                rs = stmt.executeQuery(sqlBusyTeams);
                if (rs.next()) stats.setBusyTeams(rs.getInt(1));
            }
        }
        return stats;
    }
    
    public List<EventStat> getEventStats() throws SQLException {
        List<EventStat> stats = new ArrayList<>();
        String sql = "SELECT de.EventID, de.EventName, de.Status, de.SeverityLevel, " +
                     "de.BudgetAllocated, COUNT(DISTINCT er.ReportID) as ReportCount, " +
                     "ISNULL(SUM(ft.Amount), 0) as TotalSpent " +
                     "FROM Disaster_Event de " +
                     "LEFT JOIN Emergency_Report er ON de.EventID = er.Eventid " +
                     "LEFT JOIN Financial_Transaction ft ON de.EventID = ft.Eventid " +
                     "GROUP BY de.EventID, de.EventName, de.Status, de.SeverityLevel, de.BudgetAllocated";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                EventStat stat = new EventStat();
                stat.setEventId(rs.getInt("EventID"));
                stat.setEventName(rs.getString("EventName"));
                stat.setStatus(rs.getString("Status"));
                stat.setSeverityLevel(rs.getInt("SeverityLevel"));
                stat.setBudgetAllocated(rs.getDouble("BudgetAllocated"));
                stat.setReportCount(rs.getInt("ReportCount"));
                stat.setTotalSpent(rs.getDouble("TotalSpent"));
                stats.add(stat);
            }
        }
        return stats;
    }
    
    public List<ReportStat> getReportStats() throws SQLException {
        List<ReportStat> stats = new ArrayList<>();
        String sql = "SELECT SeverityLevel, COUNT(*) as Count, " +
                     "SUM(CASE WHEN Status IN ('Pending', 'Active', 'Assigned') THEN 1 ELSE 0 END) as ActiveCount " +
                     "FROM Emergency_Report GROUP BY SeverityLevel ORDER BY SeverityLevel";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                ReportStat stat = new ReportStat();
                stat.setSeverityLevel(rs.getInt("SeverityLevel"));
                stat.setCount(rs.getInt("Count"));
                stat.setActiveCount(rs.getInt("ActiveCount"));
                stats.add(stat);
            }
        }
        return stats;
    }
}
