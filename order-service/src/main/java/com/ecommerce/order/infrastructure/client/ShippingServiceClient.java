package com.ecommerce.order.infrastructure.client;

import com.ecommerce.order.domain.exception.ExternalServiceUnavailableException;
import com.ecommerce.order.domain.port.ShipmentGateway;
import com.ecommerce.order.infrastructure.client.dto.ShipmentStatusResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.UUID;

/** REST adapter for the Shipping Service (port: {@link ShipmentGateway}). */
@Component
public class ShippingServiceClient implements ShipmentGateway {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ShippingServiceClient(RestTemplate restTemplate,
                                 @Value("${shipping-service.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @Override
    public Optional<String> findShipmentStatus(UUID orderId) {
        try {
            ShipmentStatusResponse response = restTemplate.getForObject(
                    baseUrl + "/shipments/{id}", ShipmentStatusResponse.class, orderId);
            return Optional.ofNullable(response).map(ShipmentStatusResponse::getStatus);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty(); // 404 means "no shipment yet", which is a normal state
        } catch (RestClientException e) {
            throw new ExternalServiceUnavailableException("Shipping Service is unavailable", e);
        }
    }
}
