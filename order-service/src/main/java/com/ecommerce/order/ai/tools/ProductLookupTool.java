package com.ecommerce.order.ai.tools;

import com.ecommerce.order.ai.dto.ProductLookupResponse;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class ProductLookupTool {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ProductLookupTool(RestTemplate restTemplate,
                              @Value("${product-service.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @Tool("Search for products by name or keyword and return matching products with price and stock")
    public String searchProducts(String keyword) {
        ProductLookupResponse[] products = restTemplate.getForObject(
                baseUrl + "/products?search=" + keyword, ProductLookupResponse[].class);

        if (products == null || products.length == 0) {
            return "No matching products found.";
        }

        StringBuilder sb = new StringBuilder();
        for (ProductLookupResponse p : products) {
            sb.append(p.getName())
              .append(" - $").append(p.getPrice())
              .append(" - ").append(p.getStockQuantity()).append(" in stock\n");
        }
        return sb.toString().trim();
    }
}
