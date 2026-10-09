package com.ecommerce.customer.service.dto;

public class CreateFeedbackRequest {
    private String content;
    private Integer rating;

    public CreateFeedbackRequest() {}

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
}
