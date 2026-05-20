package com.disaster.model;

public class ReportStat {
    private int severityLevel;
    private int count;
    private int activeCount;

    public ReportStat() {}

    public int getSeverityLevel() { return severityLevel; }
    public void setSeverityLevel(int severityLevel) { this.severityLevel = severityLevel; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    public int getActiveCount() { return activeCount; }
    public void setActiveCount(int activeCount) { this.activeCount = activeCount; }
}
