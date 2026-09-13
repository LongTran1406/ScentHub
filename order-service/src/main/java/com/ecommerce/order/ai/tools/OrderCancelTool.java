package com.ecommerce.order.ai.tools;

import com.ecommerce.order.exception.OrderCancellationNotAllowedException;
import com.ecommerce.order.exception.OrderNotFoundException;
import com.ecommerce.order.service.OrderService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OrderCancelTool {

    private final OrderService orderService;

    public OrderCancelTool(OrderService orderService) {
        this.orderService = orderService;
    }

    @Tool("Cancel an order by its ID. Only works if the order hasn't shipped yet.")
    public String cancelOrder(String orderId) {
        try {
            orderService.cancelOrder(UUID.fromString(orderId));
            return "Order " + orderId + " has been successfully cancelled.";
        } catch (OrderCancellationNotAllowedException e) {
            return e.getMessage();
        } catch (OrderNotFoundException e) {
            return "No order found with ID " + orderId;
        }
    }
}
