package com.ecommerce.inventory.infrastructure.messaging.dto;

public class EventEnvelope {

    private String eventType;
    private String eventId;
    private String timestamp;
    private Object payload;

    public EventEnvelope() {}

    public EventEnvelope(String eventType, String eventId, String timestamp, Object payload) {
        this.eventType = eventType;
        this.eventId = eventId;
        this.timestamp = timestamp;
        this.payload = payload;
    }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }
}
