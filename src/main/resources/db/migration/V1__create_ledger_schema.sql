CREATE TABLE ledger_entries (
    entry_id        BIGSERIAL,
    transaction_id  TEXT NOT NULL,
    account_id      TEXT NOT NULL,
    entry_type      TEXT NOT NULL
                    CHECK (entry_type IN ('charge', 'credit', 'refund', 'adjustment')),
    amount_cents    BIGINT NOT NULL
                    CHECK (amount_cents > 0),
    currency        TEXT NOT NULL DEFAULT 'USD',
    metadata        JSONB,
    partition_id    TEXT,
    sequence_number BIGINT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (entry_id, created_at)
) PARTITION BY RANGE (created_at);

CREATE UNIQUE INDEX idx_ledger_txn_id
    ON ledger_entries(transaction_id, created_at);

CREATE INDEX idx_ledger_account_time
    ON ledger_entries(account_id, created_at DESC);

CREATE INDEX idx_ledger_type_time
    ON ledger_entries(entry_type, created_at DESC);

CREATE TABLE account_balances (
    account_id          TEXT PRIMARY KEY,
    balance_cents       BIGINT NOT NULL DEFAULT 0,
    total_charges       BIGINT NOT NULL DEFAULT 0,
    total_credits       BIGINT NOT NULL DEFAULT 0,
    total_refunds       BIGINT NOT NULL DEFAULT 0,
    transaction_count   INT NOT NULL DEFAULT 0,
    last_entry_id       BIGINT,
    last_transaction_id TEXT,
    updated_at          TIMESTAMPTZ DEFAULT now()
);
