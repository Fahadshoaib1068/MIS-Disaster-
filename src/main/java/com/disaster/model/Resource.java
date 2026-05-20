package com.disaster.model;

public class Resource {
    private int resourceId;
    private String resourceName;
    private String resourceType;
    private String unit;
    private int thresholdLevel;
    
    public Resource() {}
    
    public int getResourceId() { return resourceId; }
    public void setResourceId(int resourceId) { this.resourceId = resourceId; }
    
    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }
    
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    
    public int getThresholdLevel() { return thresholdLevel; }
    public void setThresholdLevel(int thresholdLevel) { this.thresholdLevel = thresholdLevel; }
}