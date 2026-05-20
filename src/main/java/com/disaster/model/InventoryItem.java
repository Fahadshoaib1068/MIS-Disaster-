package com.disaster.model;

public class InventoryItem {
    private int warehouseId;
    private String warehouseName;
    private String city;
    private int warehouseCapacity;
    private int resourceId;
    private String resourceName;
    private String resourceType;
    private String unit;
    private int quantityAvailable;
    private int quantityDispatched;
    private int quantityConsumed;
    private int thresholdLevel;
    private String lastUpdated;

    public InventoryItem() {}

    public int getWarehouseId() { return warehouseId; }
    public void setWarehouseId(int warehouseId) { this.warehouseId = warehouseId; }

    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public int getWarehouseCapacity() { return warehouseCapacity; }
    public void setWarehouseCapacity(int warehouseCapacity) { this.warehouseCapacity = warehouseCapacity; }

    public int getResourceId() { return resourceId; }
    public void setResourceId(int resourceId) { this.resourceId = resourceId; }

    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public int getQuantityAvailable() { return quantityAvailable; }
    public void setQuantityAvailable(int quantityAvailable) { this.quantityAvailable = quantityAvailable; }

    public int getQuantityDispatched() { return quantityDispatched; }
    public void setQuantityDispatched(int quantityDispatched) { this.quantityDispatched = quantityDispatched; }

    public int getQuantityConsumed() { return quantityConsumed; }
    public void setQuantityConsumed(int quantityConsumed) { this.quantityConsumed = quantityConsumed; }

    public int getThresholdLevel() { return thresholdLevel; }
    public void setThresholdLevel(int thresholdLevel) { this.thresholdLevel = thresholdLevel; }

    public String getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }
}
