package com.ecommerce.customer.service.dto;

import java.util.UUID;

public class ComplaintResponse {
    private UUID complaintId;
    private String content;
    private String status;
    private String createdAt;

    public ComplaintResponse() {}

    public ComplaintResponse(UUID complaintId, String content, String status, String createdAt) {
        this.complaintId = complaintId;
        this.content = content;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getComplaintId() { return complaintId; }
    public void setComplaintId(UUID complaintId) { this.complaintId = complaintId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
