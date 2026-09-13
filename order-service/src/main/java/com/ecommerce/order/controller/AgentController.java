package com.ecommerce.order.controller;

import com.ecommerce.order.ai.agent.CancelSupportAgent;
import com.ecommerce.order.ai.agent.ProductConsultAgent;
import com.ecommerce.order.dto.AgentChatRequest;
import com.ecommerce.order.dto.AgentChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/agents")
public class AgentController {

    private static final Logger log = LoggerFactory.getLogger(AgentController.class);
    private static final String FALLBACK_REPLY =
            "Sorry, the assistant is temporarily unavailable. Please try again later.";

    private final ProductConsultAgent productConsultAgent;
    private final CancelSupportAgent cancelSupportAgent;

    public AgentController(ProductConsultAgent productConsultAgent, CancelSupportAgent cancelSupportAgent) {
        this.productConsultAgent = productConsultAgent;
        this.cancelSupportAgent = cancelSupportAgent;
    }

    @PostMapping("/product-consult")
    public ResponseEntity<AgentChatResponse> productConsult(@RequestBody AgentChatRequest request) {
        try {
            String reply = productConsultAgent.chat(request.getMessage());
            return ResponseEntity.ok(new AgentChatResponse(reply));
        } catch (Exception e) {
            log.error("Product consult agent failed: {}", e.getMessage(), e);
            return ResponseEntity.ok(new AgentChatResponse(FALLBACK_REPLY));
        }
    }

    @PostMapping("/cancel-support")
    public ResponseEntity<AgentChatResponse> cancelSupport(@RequestBody AgentChatRequest request) {
        try {
            String reply = cancelSupportAgent.chat(request.getMessage());
            return ResponseEntity.ok(new AgentChatResponse(reply));
        } catch (Exception e) {
            log.error("Cancel support agent failed: {}", e.getMessage(), e);
            return ResponseEntity.ok(new AgentChatResponse(FALLBACK_REPLY));
        }
    }
}
