package com.ecommerce.order.domain.port;

import java.util.UUID;

/** Port to the Customer context. Implementations throw ExternalServiceUnavailableException if it is down. */
public interface CustomerDirectory {

    boolean exists(UUID customerId);
}
