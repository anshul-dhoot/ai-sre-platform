# ADR-002 — Kafka Event Transport

## Status

Accepted

## Context

The Order Service needs to publish `OrderCreated` events so downstream services, such as the Payment Service, can consume them asynchronously. The v1 transport must support a local demonstration while establishing clear event identity, ordering, and duplicate-handling expectations.

## Decision

- Kafka is the event transport for v1.
- The topic is `orders.events`, with two partitions for the local v1 demonstration.
- The Order Service is the producer. The Payment Service will eventually consume these events.
- Kafka records use `orderId` as the key. Events for the same order are routed to the same partition, preserving their order within that partition.
- `eventId` identifies an event instance and supports consumer deduplication/idempotency; it is not the Kafka key.
- Events use JSON serialization. Both `orderId` and `eventId` are UUIDs in v1.
- Delivery is at-least-once. Consumers must tolerate duplicate delivery and may use `eventId` as an idempotency key.
- v1 deliberately excludes Avro, Protobuf, Schema Registry, Kafka Streams, transactions or exactly-once processing, and multiple Order Service instances.

## Rationale

Kafka provides the asynchronous event transport needed by the order flow. Keying records by `orderId` keeps each order's events together for partition-level ordering, while a distinct `eventId` lets consumers recognize repeated delivery of the same event. JSON and two local partitions keep the demonstration straightforward; at-least-once delivery makes duplicate handling an explicit consumer responsibility.

## Consequences

- The Payment Service can be added later as an asynchronous consumer of `orders.events`.
- Ordering is guaranteed only for events with the same key in their partition; no ordering across partitions is implied.
- Consumers need idempotent handling because an event may be delivered more than once.
- The v1 format and deployment choices do not provide schema governance, stream processing, exactly-once guarantees, or producer scaling across multiple Order Service instances.
