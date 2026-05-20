package com.disaster.dao;

import com.disaster.model.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TeamDAO {
    private final ApprovalDAO approvalDAO = new ApprovalDAO();
    
    public List<RescueTeam> getAllTeams() throws SQLException {
        List<RescueTeam> teams = new ArrayList<>();
        String sql = "SELECT * FROM Rescue_Team ORDER BY TeamID";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                RescueTeam team = new RescueTeam();
                team.setTeamId(rs.getInt("TeamID"));
                team.setTeamName(rs.getString("Teamname"));
                team.setTeamType(rs.getString("TeamType"));
                team.setCurrentLat(rs.getDouble("CurrentLat"));
                team.setCurrentLong(rs.getDouble("CurrentLong"));
                team.setAvailabilityStatus(rs.getString("AvailabilityStatus"));
                team.setCapacity(rs.getInt("Capacity"));
                teams.add(team);
            }
        }
        return teams;
    }
    
    public List<RescueTeam> getAvailableTeams() throws SQLException {
        List<RescueTeam> teams = new ArrayList<>();
        String sql = "SELECT * FROM Rescue_Team WHERE AvailabilityStatus = 'Available'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                RescueTeam team = new RescueTeam();
                team.setTeamId(rs.getInt("TeamID"));
                team.setTeamName(rs.getString("Teamname"));
                team.setTeamType(rs.getString("TeamType"));
                team.setCurrentLat(rs.getDouble("CurrentLat"));
                team.setCurrentLong(rs.getDouble("CurrentLong"));
                team.setAvailabilityStatus(rs.getString("AvailabilityStatus"));
                team.setCapacity(rs.getInt("Capacity"));
                teams.add(team);
            }
        }
        return teams;
    }
    
    public boolean assignTeamToReport(int teamId, int reportId, Integer authorizedBy, int requestedBy) throws SQLException {
        String sql = "INSERT INTO Team_Assignment (Teamid, Reportid, Authorizedby, Status) " +
                     "VALUES (?, ?, ?, 'Active')";
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, teamId);
                pstmt.setInt(2, reportId);
                if (authorizedBy != null) {
                    pstmt.setInt(3, authorizedBy);
                } else {
                    pstmt.setNull(3, Types.INTEGER);
                }

                if (pstmt.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                int assignmentId;
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (!rs.next()) {
                        conn.rollback();
                        throw new SQLException("Failed to create team deployment request.");
                    }
                    assignmentId = rs.getInt(1);
                }

                approvalDAO.createApprovalRequest(conn, "RescueDeployment", assignmentId, requestedBy);
                insertAuditLog(conn, "Team_Assignment", "INSERT", null, "AssignmentID=" + assignmentId + ",Status=PendingApproval", requestedBy);

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
    
    public boolean updateTeamStatus(int teamId, String status) throws SQLException {
        String sql = "UPDATE Rescue_Team SET AvailabilityStatus = ? WHERE TeamID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, status);
            pstmt.setInt(2, teamId);
            return pstmt.executeUpdate() > 0;
        }
    }
    
    public List<TeamAssignment> getTeamAssignments() throws SQLException {
        List<TeamAssignment> assignments = new ArrayList<>();
        String sql = "SELECT ta.*, rt.Teamname, er.Address as ReportLocation " +
                     "FROM Team_Assignment ta " +
                     "JOIN Rescue_Team rt ON ta.Teamid = rt.TeamID " +
                     "JOIN Emergency_Report er ON ta.Reportid = er.ReportID " +
                     "LEFT JOIN Approval_Request ar ON ar.Referenceid = ta.AssignmentID AND ar.Requesttype = 'RescueDeployment' " +
                     "WHERE ar.Approvalid IS NULL OR ar.Status = 'Approved' " +
                     "ORDER BY ta.Assignedat DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                TeamAssignment assignment = new TeamAssignment();
                assignment.setAssignmentId(rs.getInt("AssignmentID"));
                assignment.setTeamId(rs.getInt("Teamid"));
                assignment.setTeamName(rs.getString("Teamname"));
                assignment.setReportId(rs.getInt("Reportid"));
                assignment.setReportLocation(rs.getString("ReportLocation"));
                assignment.setAuthorizedBy(rs.getInt("Authorizedby"));
                assignment.setAssignedAt(rs.getString("Assignedat"));
                assignment.setCompletedAt(rs.getString("Completedat"));
                assignment.setStatus(rs.getString("Status"));
                assignments.add(assignment);
            }
        }
        return assignments;
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
