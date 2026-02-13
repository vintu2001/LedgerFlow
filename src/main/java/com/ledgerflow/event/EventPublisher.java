package com.ledgerflow.event;

import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventDataBatch;
import com.azure.messaging.eventhubs.EventHubProducerClient;
import com.azure.messaging.eventhubs.models.CreateBatchOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerflow.model.TransactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final EventHubProducerClient producerClient;
    private final ObjectMapper objectMapper;

    public EventPublisher(@Autowired(required = false) EventHubProducerClient producerClient,
                          ObjectMapper objectMapper) {
        this.producerClient = producerClient;
        this.objectMapper = objectMapper;
    }

    public void publish(TransactionEvent event) {
        if (producerClient == null) {
            throw new IllegalStateException(
                "Event Hub producer is not configured. Set EVENTHUB_CONNECTION_STRING.");
        }

        try {
            byte[] payload = objectMapper.writeValueAsBytes(event);
            EventData eventData = new EventData(payload);
            eventData.getProperties().put("transactionId", event.transactionId());
            eventData.getProperties().put("entryType", event.entryType());

            CreateBatchOptions options = new CreateBatchOptions()
                .setPartitionKey(event.accountId());

            EventDataBatch batch = producerClient.createBatch(options);
            if (!batch.tryAdd(eventData)) {
                throw new RuntimeException("Event too large for batch");
            }

            producerClient.send(batch);
            log.info("Published transaction {} for account {}",
                event.transactionId(), event.accountId());
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to publish transaction {}: {}",
                event.transactionId(), e.getMessage());
            throw new RuntimeException("Event publishing failed", e);
        }
    }
}
