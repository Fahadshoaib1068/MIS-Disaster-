package com.disaster.model;

public class TeamAssignment {
    private int assignmentId;
    private int teamId;
    private String teamName;
    private int reportId;
    private String reportLocation;
    private int authorizedBy;
    private String assignedAt;
    private String completedAt;
    private String status;

    public TeamAssignment() {}

    public int getAssignmentId() { return assignmentId; }
    public void setAssignmentId(int assignmentId) { this.assignmentId = assignmentId; }

    public int getTeamId() { return teamId; }
    public void setTeamId(int teamId) { this.teamId = teamId; }

    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }

    public int getReportId() { return reportId; }
    public void setReportId(int reportId) { this.reportId = reportId; }

    public String getReportLocation() { return reportLocation; }
    public void setReportLocation(String reportLocation) { this.reportLocation = reportLocation; }

    public int getAuthorizedBy() { return authorizedBy; }
    public void setAuthorizedBy(int authorizedBy) { this.authorizedBy = authorizedBy; }

    public String getAssignedAt() { return assignedAt; }
    public void setAssignedAt(String assignedAt) { this.assignedAt = assignedAt; }

    public String getCompletedAt() { return completedAt; }
    public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
