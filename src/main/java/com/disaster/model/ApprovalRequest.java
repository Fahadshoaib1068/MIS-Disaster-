package com.disaster.model;

public class ApprovalRequest {
    private int approvalId;
    private String requestType;
    private String requestedAt;
    private String status;
    private String decisionAt;
    private String comments;
    private int referenceId;
    private int requestedBy;
    private int approvedBy;
    private String requestedByName;
    private String approvedByName;
    
    public ApprovalRequest() {}
    
    public int getApprovalId() { return approvalId; }
    public void setApprovalId(int approvalId) { this.approvalId = approvalId; }
    
    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }
    
    public String getRequestedAt() { return requestedAt; }
    public void setRequestedAt(String requestedAt) { this.requestedAt = requestedAt; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getDecisionAt() { return decisionAt; }
    public void setDecisionAt(String decisionAt) { this.decisionAt = decisionAt; }
    
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    
    public int getReferenceId() { return referenceId; }
    public void setReferenceId(int referenceId) { this.referenceId = referenceId; }
    
    public int getRequestedBy() { return requestedBy; }
    public void setRequestedBy(int requestedBy) { this.requestedBy = requestedBy; }
    
    public int getApprovedBy() { return approvedBy; }
    public void setApprovedBy(int approvedBy) { this.approvedBy = approvedBy; }

    public String getRequestedByName() { return requestedByName; }
    public void setRequestedByName(String requestedByName) { this.requestedByName = requestedByName; }

    public String getApprovedByName() { return approvedByName; }
    public void setApprovedByName(String approvedByName) { this.approvedByName = approvedByName; }
}
