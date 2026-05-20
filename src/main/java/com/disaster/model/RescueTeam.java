package com.disaster.model;

public class RescueTeam {
    private int teamId;
    private String teamName;
    private String teamType;
    private double currentLat;
    private double currentLong;
    private String availabilityStatus;
    private int capacity;
    
    public RescueTeam() {}
    
    public int getTeamId() { return teamId; }
    public void setTeamId(int teamId) { this.teamId = teamId; }
    
    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
    
    public String getTeamType() { return teamType; }
    public void setTeamType(String teamType) { this.teamType = teamType; }
    
    public double getCurrentLat() { return currentLat; }
    public void setCurrentLat(double currentLat) { this.currentLat = currentLat; }
    
    public double getCurrentLong() { return currentLong; }
    public void setCurrentLong(double currentLong) { this.currentLong = currentLong; }
    
    public String getAvailabilityStatus() { return availabilityStatus; }
    public void setAvailabilityStatus(String availabilityStatus) { this.availabilityStatus = availabilityStatus; }
    
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
}