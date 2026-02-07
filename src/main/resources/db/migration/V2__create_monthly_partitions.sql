CREATE TABLE ledger_entries_2026_01
    PARTITION OF ledger_entries
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');

CREATE TABLE ledger_entries_2026_02
    PARTITION OF ledger_entries
    FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');

CREATE TABLE ledger_entries_2026_03
    PARTITION OF ledger_entries
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');

CREATE TABLE ledger_entries_2026_04
    PARTITION OF ledger_entries
    FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');

CREATE TABLE ledger_entries_2026_05
    PARTITION OF ledger_entries
    FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');

CREATE TABLE ledger_entries_2026_06
    PARTITION OF ledger_entries
    FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');

CREATE TABLE ledger_entries_default
    PARTITION OF ledger_entries DEFAULT;
