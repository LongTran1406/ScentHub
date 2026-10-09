package com.ecommerce.order.presentation.dto;

public class AgentChatResponse {

    private String reply;

    public AgentChatResponse() {}

    public AgentChatResponse(String reply) {
        this.reply = reply;
    }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }
}
