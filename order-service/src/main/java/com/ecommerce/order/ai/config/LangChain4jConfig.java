package com.ecommerce.order.ai.config;

import com.ecommerce.order.ai.agent.CancelSupportAgent;
import com.ecommerce.order.ai.agent.ProductConsultAgent;
import com.ecommerce.order.ai.tools.OrderCancelTool;
import com.ecommerce.order.ai.tools.ProductLookupTool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class LangChain4jConfig {

    @Bean
    public ChatModel chatModel(
            @Value("${groq.api-key}") String apiKey,
            @Value("${groq.base-url}") String baseUrl,
            @Value("${groq.model-name}") String modelName) {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .timeout(Duration.ofSeconds(30))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Bean
    public ProductConsultAgent productConsultAgent(ChatModel chatModel,
                                                     ProductLookupTool productLookupTool) {
        return AiServices.builder(ProductConsultAgent.class)
                .chatModel(chatModel)
                .tools(productLookupTool)
                .build();
    }

    @Bean
    public CancelSupportAgent cancelSupportAgent(ChatModel chatModel,
                                                  OrderCancelTool orderCancelTool) {
        return AiServices.builder(CancelSupportAgent.class)
                .chatModel(chatModel)
                .tools(orderCancelTool)
                .build();
    }
}
