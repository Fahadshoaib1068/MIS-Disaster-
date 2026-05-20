package com.disaster.model;

public class DisasterEvent {
    private int eventId;
    private String eventName;
    private String disasterType;
    private String affectedRegion;
    private String startDate;
    private String endDate;
    private int severityLevel;
    private String status;
    private double budgetAllocated;
    
    public DisasterEvent() {}
    
    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }
    
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public String getDisasterType() { return disasterType; }
    public void setDisasterType(String disasterType) { this.disasterType = disasterType; }
    
    public String getAffectedRegion() { return affectedRegion; }
    public void setAffectedRegion(String affectedRegion) { this.affectedRegion = affectedRegion; }
    
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    
    public int getSeverityLevel() { return severityLevel; }
    public void setSeverityLevel(int severityLevel) { this.severityLevel = severityLevel; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public double getBudgetAllocated() { return budgetAllocated; }
    public void setBudgetAllocated(double budgetAllocated) { this.budgetAllocated = budgetAllocated; }
}
