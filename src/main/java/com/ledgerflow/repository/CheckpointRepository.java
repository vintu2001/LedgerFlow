package com.ledgerflow.repository;

import com.ledgerflow.model.EventCheckpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CheckpointRepository extends JpaRepository<EventCheckpoint, Long> {

    Optional<EventCheckpoint> findByConsumerGroupAndPartitionId(
            String consumerGroup, String partitionId);
}
