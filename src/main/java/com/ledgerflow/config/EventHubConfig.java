package com.ledgerflow.config;

import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubConsumerAsyncClient;
import com.azure.messaging.eventhubs.EventHubProducerClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventHubConfig {

    @Bean
    @ConditionalOnExpression("!'${azure.eventhub.connection-string:}'.trim().isEmpty()")
    public EventHubProducerClient eventHubProducerClient(
            @Value("${azure.eventhub.connection-string}") String connectionString,
            @Value("${azure.eventhub.name}") String eventHubName) {
        return new EventHubClientBuilder()
            .connectionString(connectionString, eventHubName)
            .buildProducerClient();
    }

    @Bean
    @ConditionalOnExpression("!'${azure.eventhub.connection-string:}'.trim().isEmpty()")
    public EventHubConsumerAsyncClient eventHubConsumerClient(
            @Value("${azure.eventhub.connection-string}") String connectionString,
            @Value("${azure.eventhub.name}") String eventHubName,
            @Value("${azure.eventhub.consumer-group}") String consumerGroup) {
        return new EventHubClientBuilder()
            .connectionString(connectionString, eventHubName)
            .consumerGroup(consumerGroup)
            .buildAsyncConsumerClient();
    }
}
