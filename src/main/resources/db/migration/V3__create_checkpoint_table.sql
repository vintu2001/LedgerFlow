CREATE TABLE event_checkpoints (
    id              BIGSERIAL PRIMARY KEY,
    consumer_group  TEXT NOT NULL,
    partition_id    TEXT NOT NULL,
    sequence_number BIGINT NOT NULL,
    updated_at      TIMESTAMPTZ DEFAULT now(),
    UNIQUE (consumer_group, partition_id)
);
