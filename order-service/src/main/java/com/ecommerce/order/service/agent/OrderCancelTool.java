package com.ecommerce.order.service.agent;

import com.ecommerce.order.domain.exception.ExternalServiceUnavailableException;
import com.ecommerce.order.domain.exception.OrderCancellationNotAllowedException;
import com.ecommerce.order.domain.exception.OrderNotFoundException;
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
        } catch (IllegalArgumentException e) {
            return "\"" + orderId + "\" is not a valid order ID.";
        } catch (ExternalServiceUnavailableException e) {
            return "The shipping system is temporarily unavailable, so the order could not be checked. Please try again later.";
        }
    }
}
