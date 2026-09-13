package com.ecommerce.order.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
public class ShippingServiceClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ShippingServiceClient(RestTemplate restTemplate,
                                  @Value("${shipping-service.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public String getShipmentStatus(UUID orderId) {
        try {
            ShipmentStatusResponse response = restTemplate.getForObject(
                    baseUrl + "/shipments/" + orderId, ShipmentStatusResponse.class);
            return response != null ? response.getStatus() : null;
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
    }
}
