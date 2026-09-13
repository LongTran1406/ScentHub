package com.ecommerce.order.ai.agent;

import dev.langchain4j.service.SystemMessage;

public interface ProductConsultAgent {

    @SystemMessage("You are a helpful shopping assistant for an e-commerce store. "
        + "Use the searchProducts tool to find real product information before "
        + "answering. Do not make up product names, prices, or stock levels.")
    String chat(String userMessage);
}
