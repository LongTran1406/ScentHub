package com.ecommerce.order.service.agent;

import dev.langchain4j.service.SystemMessage;

public interface CancelSupportAgent {

    @SystemMessage("You are a customer support assistant that helps customers "
        + "cancel orders. Ask for the order ID if not provided. Use the "
        + "cancelOrder tool to attempt cancellation, then relay the exact "
        + "result to the customer in a friendly tone. Do not claim an order "
        + "was cancelled unless the tool confirms it.")
    String chat(String userMessage);
}
