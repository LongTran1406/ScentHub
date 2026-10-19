package com.ecommerce.order.service.agent;

import com.ecommerce.order.domain.port.ProductCatalog;
import com.ecommerce.order.domain.port.ProductSnapshot;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

/** LangChain4j tool: lets the product-consultation agent read REAL product data from the Inventory context. */
@Component
public class ProductLookupTool {

    private final ProductCatalog productCatalog;

    public ProductLookupTool(ProductCatalog productCatalog) {
        this.productCatalog = productCatalog;
    }

    @Tool("Search for products by ONE short keyword such as a product name or scent (for example 'rose'), "
        + "not a full sentence or several words, and return matching products with price and stock")
    public String searchProducts(String keyword) {
        List<ProductSnapshot> products = productCatalog.searchProducts(keyword);
        if (products.isEmpty()) {
            return "No matching products found.";
        }

        StringBuilder sb = new StringBuilder();
        for (ProductSnapshot p : products) {
            sb.append(p.name())
              .append(" - $").append(p.price())
              .append(" - ").append(p.availableQuantity()).append(" in stock\n");
        }
        return sb.toString().trim();
    }
}
