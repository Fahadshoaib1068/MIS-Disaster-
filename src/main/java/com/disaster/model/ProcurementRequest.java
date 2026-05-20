package com.disaster.model;

public class ProcurementRequest {
    private int warehouseId;
    private int resourceId;
    private int requestedQty;
    private String reason;
    private int requestedBy;

    public ProcurementRequest() {}

    public int getWarehouseId() { return warehouseId; }
    public void setWarehouseId(int warehouseId) { this.warehouseId = warehouseId; }

    public int getResourceId() { return resourceId; }
    public void setResourceId(int resourceId) { this.resourceId = resourceId; }

    public int getRequestedQty() { return requestedQty; }
    public void setRequestedQty(int requestedQty) { this.requestedQty = requestedQty; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public int getRequestedBy() { return requestedBy; }
    public void setRequestedBy(int requestedBy) { this.requestedBy = requestedBy; }
}
