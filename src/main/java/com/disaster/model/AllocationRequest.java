package com.disaster.model;

public class AllocationRequest {
    private int allocationId;
    private int resourceId;
    private String resourceName;
    private int warehouseId;
    private String warehouseName;
    private int eventId;
    private String eventName;
    private int requestedBy;
    private String requestedByName;
    private int requestedQty;
    private Integer approvedQty;
    private String allocationDate;
    private String status;
    private String purpose;
    private Integer approvalId;
    private String approvalStatus;
    private Integer approvalRequestedBy;
    private String approvalRequestedByName;
    private String approvalComments;
    private int approvalCount;
    
    public AllocationRequest() {}
    
    // Getters and Setters
    public int getAllocationId() { return allocationId; }
    public void setAllocationId(int allocationId) { this.allocationId = allocationId; }
    
    public int getResourceId() { return resourceId; }
    public void setResourceId(int resourceId) { this.resourceId = resourceId; }
    
    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }
    
    public int getWarehouseId() { return warehouseId; }
    public void setWarehouseId(int warehouseId) { this.warehouseId = warehouseId; }
    
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }
    
    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }
    
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    
    public int getRequestedBy() { return requestedBy; }
    public void setRequestedBy(int requestedBy) { this.requestedBy = requestedBy; }
    
    public String getRequestedByName() { return requestedByName; }
    public void setRequestedByName(String requestedByName) { this.requestedByName = requestedByName; }
    
    public int getRequestedQty() { return requestedQty; }
    public void setRequestedQty(int requestedQty) { this.requestedQty = requestedQty; }
    
    public Integer getApprovedQty() { return approvedQty; }
    public void setApprovedQty(Integer approvedQty) { this.approvedQty = approvedQty; }
    
    public String getAllocationDate() { return allocationDate; }
    public void setAllocationDate(String allocationDate) { this.allocationDate = allocationDate; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public Integer getApprovalId() { return approvalId; }
    public void setApprovalId(Integer approvalId) { this.approvalId = approvalId; }

    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }

    public Integer getApprovalRequestedBy() { return approvalRequestedBy; }
    public void setApprovalRequestedBy(Integer approvalRequestedBy) { this.approvalRequestedBy = approvalRequestedBy; }

    public String getApprovalRequestedByName() { return approvalRequestedByName; }
    public void setApprovalRequestedByName(String approvalRequestedByName) { this.approvalRequestedByName = approvalRequestedByName; }

    public String getApprovalComments() { return approvalComments; }
    public void setApprovalComments(String approvalComments) { this.approvalComments = approvalComments; }

    public int getApprovalCount() { return approvalCount; }
    public void setApprovalCount(int approvalCount) { this.approvalCount = approvalCount; }
}
