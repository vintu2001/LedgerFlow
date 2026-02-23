# LedgerFlow

Event-Sourced Billing Ledger built with Java 17, Spring Boot, Azure Event Hubs, and PostgreSQL.

## Overview

LedgerFlow is a distributed billing ledger that records financial transactions (charges, credits, refunds) as immutable events through Azure Event Hubs, materializes account balances in PostgreSQL, and provides ACID-compliant balance queries with sub-50ms validation latency.

## Architecture

```
Client → REST API → TransactionService → Azure Event Hubs (partitioned by accountId)
                                                    ↓
                                            EventConsumerService
                                                    ↓
                                            LedgerProcessor (@Transactional)
                                                    ↓
                                    ┌───────────────┼───────────────┐
                                    ↓               ↓               ↓
                            ledger_entries   account_balances  event_checkpoints
                           (partitioned)      (materialized)   (exactly-once)
```

## Key Design Decisions

- **Event sourcing** with Azure Event Hubs for durability and replayability
- **Immutable ledger entries** (append-only) for audit compliance
- **Idempotent processing** via unique transaction_id constraint
- **Atomic balance materialization** with PostgreSQL UPSERT
- **Monthly table partitioning** for sub-50ms query performance under high concurrency
- **Exactly-once semantics** via DB-stored checkpoints within the same transaction

## Prerequisites

- Java 17
- Maven 3.9+
- Docker and Docker Compose
- Azure Event Hubs namespace

## Getting Started

### Local Database Setup

```bash
docker-compose up -d postgres
```

### Environment Variables

```bash
export EVENTHUB_CONNECTION_STRING="Endpoint=sb://<namespace>.servicebus.windows.net/;SharedAccessKeyName=...;SharedAccessKey=..."
export EVENTHUB_NAME=transactions
```

### Build and Run

```bash
mvn clean package -DskipTests
java -jar target/ledgerflow-1.0.0.jar
```

## API Endpoints

### Submit Transaction

```
POST /api/transactions
Content-Type: application/json

{
  "accountId": "acct-123",
  "entryType": "charge",
  "amountCents": 1500,
  "currency": "USD",
  "metadata": "{\"description\": \"compute usage\"}"
}
```

### Get Account Balance

```
GET /api/accounts/{accountId}/balance
```

### Get Account Statement

```
GET /api/accounts/{accountId}/statements?from=2026-01-01T00:00:00Z&to=2026-02-01T00:00:00Z
```

### Get Billing Report

```
GET /api/accounts/{accountId}/reports?from=2026-01-01T00:00:00Z&to=2026-02-01T00:00:00Z
```

## Testing

```bash
mvn test
```

## Database Schema

The schema is managed by Flyway migrations. The ledger uses PostgreSQL range partitioning by `created_at` (monthly) to enable partition pruning for time-bounded billing queries.

### Tables

- `ledger_entries` — Append-only immutable audit trail, partitioned by month
- `account_balances` — Materialized balance view, atomically updated with each ledger write
- `event_checkpoints` — Tracks consumed Event Hub sequence numbers for exactly-once processing

## Docker

```bash
docker-compose up --build
```
