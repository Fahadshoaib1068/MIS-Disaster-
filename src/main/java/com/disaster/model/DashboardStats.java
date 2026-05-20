package com.disaster.model;

public class DashboardStats {
    private int activeEvents;
    private int openReports;
    private int deployedTeams;
    private int pendingApprovals;
    private int patientsAdmitted;
    private int lowStockAlerts;
    private double totalDonations;
    private double totalExpenses;
    private int availableTeams;
    private int busyTeams;
    
    public DashboardStats() {}
    
    // Getters and Setters
    public int getActiveEvents() { return activeEvents; }
    public void setActiveEvents(int activeEvents) { this.activeEvents = activeEvents; }
    
    public int getOpenReports() { return openReports; }
    public void setOpenReports(int openReports) { this.openReports = openReports; }
    
    public int getDeployedTeams() { return deployedTeams; }
    public void setDeployedTeams(int deployedTeams) { this.deployedTeams = deployedTeams; }
    
    public int getPendingApprovals() { return pendingApprovals; }
    public void setPendingApprovals(int pendingApprovals) { this.pendingApprovals = pendingApprovals; }
    
    public int getPatientsAdmitted() { return patientsAdmitted; }
    public void setPatientsAdmitted(int patientsAdmitted) { this.patientsAdmitted = patientsAdmitted; }
    
    public int getLowStockAlerts() { return lowStockAlerts; }
    public void setLowStockAlerts(int lowStockAlerts) { this.lowStockAlerts = lowStockAlerts; }
    
    public double getTotalDonations() { return totalDonations; }
    public void setTotalDonations(double totalDonations) { this.totalDonations = totalDonations; }
    
    public double getTotalExpenses() { return totalExpenses; }
    public void setTotalExpenses(double totalExpenses) { this.totalExpenses = totalExpenses; }
    
    public int getAvailableTeams() { return availableTeams; }
    public void setAvailableTeams(int availableTeams) { this.availableTeams = availableTeams; }
    
    public int getBusyTeams() { return busyTeams; }
    public void setBusyTeams(int busyTeams) { this.busyTeams = busyTeams; }
}