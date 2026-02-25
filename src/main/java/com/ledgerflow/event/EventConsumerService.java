package com.ledgerflow.event;

import com.azure.messaging.eventhubs.EventHubConsumerAsyncClient;
import com.azure.messaging.eventhubs.models.EventPosition;
import com.azure.messaging.eventhubs.models.PartitionEvent;
import com.ledgerflow.repository.CheckpointRepository;
import com.ledgerflow.service.LedgerProcessor;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;

import java.util.ArrayList;
import java.util.List;

@Service
public class EventConsumerService {

    private static final Logger log = LoggerFactory.getLogger(EventConsumerService.class);

    private final EventHubConsumerAsyncClient consumerClient;
    private final LedgerProcessor ledgerProcessor;
    private final CheckpointRepository checkpointRepo;
    private final String consumerGroup;
    private final boolean enabled;
    private final List<Disposable> subscriptions = new ArrayList<>();

    public EventConsumerService(
            @Autowired(required = false) EventHubConsumerAsyncClient consumerClient,
            LedgerProcessor ledgerProcessor,
            CheckpointRepository checkpointRepo,
            @Value("${azure.eventhub.consumer-group:$Default}") String consumerGroup,
            @Value("${ledgerflow.consumer.enabled:true}") boolean enabled) {
        this.consumerClient = consumerClient;
        this.ledgerProcessor = ledgerProcessor;
        this.checkpointRepo = checkpointRepo;
        this.consumerGroup = consumerGroup;
        this.enabled = enabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startConsuming() {
        if (!enabled || consumerClient == null) {
            log.info("Event consumer is disabled or Event Hub is not configured");
            return;
        }

        consumerClient.getEventHubProperties()
            .doOnSuccess(properties -> {
                java.util.List<String> partitionIds = new java.util.ArrayList<>();
                properties.getPartitionIds().forEach(partitionIds::add);
                log.info("Connected to Event Hub: {}, partitions: {}",
                    properties.getName(), partitionIds.size());

                for (String partitionId : partitionIds) {
                    EventPosition startPosition = resolveStartPosition(partitionId);
                    Disposable subscription = consumerClient
                        .receiveFromPartition(partitionId, startPosition)
                        .subscribe(
                            partitionEvent -> processEvent(partitionId, partitionEvent),
                            error -> log.error("Error receiving from partition {}: {}",
                                partitionId, error.getMessage())
                        );
                    subscriptions.add(subscription);
                    log.info("Started consuming from partition {}", partitionId);
                }
            })
            .doOnError(error -> log.error("Failed to connect to Event Hub: {}", error.getMessage()))
            .subscribe();
    }

    private EventPosition resolveStartPosition(String partitionId) {
        return checkpointRepo.findByConsumerGroupAndPartitionId(consumerGroup, partitionId)
            .map(cp -> EventPosition.fromSequenceNumber(cp.getSequenceNumber(), false))
            .orElse(EventPosition.earliest());
    }

    private void processEvent(String partitionId, PartitionEvent partitionEvent) {
        var eventData = partitionEvent.getData();
        try {
            ledgerProcessor.process(
                eventData.getBodyAsString(),
                partitionId,
                eventData.getSequenceNumber()
            );
        } catch (Exception e) {
            log.error("Failed to process event on partition {} at sequence {}: {}",
                partitionId, eventData.getSequenceNumber(), e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        subscriptions.forEach(Disposable::dispose);
        if (consumerClient != null) {
            consumerClient.close();
        }
        log.info("Event consumer shut down");
    }
}
