package com.ecommerce.shipping;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=localhost:9999",
    "spring.kafka.consumer.auto-offset-reset=earliest"
})
class ShippingServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
