# ADR-003 — Payment Service Kafka Consumer

## Status

Accepted

## Context

The Payment Service consumes the Order Service's `OrderCreated` events from `orders.events`. This phase simulates processing locally and has no payment gateway or durable store.

## Decision

- Use the `payment-service` consumer group, one application instance, and listener concurrency one. The existing topic configuration is unchanged.
- Deserialize the producer's v1 JSON contract: `eventId` (UUID), `eventVersion` (integer), `eventType` (string), `occurredAt` (ISO 8601 UTC `Instant`), `orderId` (UUID string), `customerId` (string), and `amount` (decimal).
- Disable Kafka auto-commit and use record acknowledgement. A record is eligible for commit after its listener returns successfully. Payment success and an already-processed duplicate return successfully; invalid events and exhausted processing failures do not.
- Retry only explicitly retryable processing exceptions, for at most three attempts total with a 250 ms delay between attempts. Permanent validation errors are not retried. After an unprocessed record escapes the listener, Spring Kafka 3.3's `CommonContainerStoppingErrorHandler` stops the listener container. No dead-letter topic is used. Restarting the service resumes from the uncommitted record.
- Track processed `orderId` values in a process-local concurrent set and add an order only after simulated processing succeeds. One listener thread is required: the concurrent set protects its data structure, but the check-process-add sequence is not atomic if listener concurrency is increased.

## Consequences and limitations

- Kafka can redeliver records, so the in-memory set suppresses duplicates only during one process lifetime. The set is lost on restart, and there is a crash window between simulated success and offset commit. This is not durable payment idempotency or a real payment guarantee.
- A permanent invalid record or exhausted transient failure stops consumption and requires operator intervention or a later recovery mechanism. It remains uncommitted and can be encountered again after restart.
- No topic, broker, database, gateway, or other infrastructure is added.
