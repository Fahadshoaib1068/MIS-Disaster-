package com.disaster.model;

public class EventStat {
    private int eventId;
    private String eventName;
    private String status;
    private int severityLevel;
    private double budgetAllocated;
    private int reportCount;
    private double totalSpent;

    public EventStat() {}

    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getSeverityLevel() { return severityLevel; }
    public void setSeverityLevel(int severityLevel) { this.severityLevel = severityLevel; }

    public double getBudgetAllocated() { return budgetAllocated; }
    public void setBudgetAllocated(double budgetAllocated) { this.budgetAllocated = budgetAllocated; }

    public int getReportCount() { return reportCount; }
    public void setReportCount(int reportCount) { this.reportCount = reportCount; }

    public double getTotalSpent() { return totalSpent; }
    public void setTotalSpent(double totalSpent) { this.totalSpent = totalSpent; }
}
