package com.ecommerce.customer.service.dto;

import java.util.UUID;

public class FeedbackResponse {
    private UUID feedbackId;
    private String content;
    private Integer rating;
    private String createdAt;

    public FeedbackResponse() {}

    public FeedbackResponse(UUID feedbackId, String content, Integer rating, String createdAt) {
        this.feedbackId = feedbackId;
        this.content = content;
        this.rating = rating;
        this.createdAt = createdAt;
    }

    public UUID getFeedbackId() { return feedbackId; }
    public void setFeedbackId(UUID feedbackId) { this.feedbackId = feedbackId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
