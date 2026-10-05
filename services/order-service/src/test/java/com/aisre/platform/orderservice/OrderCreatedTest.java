package com.aisre.platform.orderservice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class OrderCreatedTest {

    @Test
    void initializesCurrentEventContractFields() {
        UUID eventId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        Instant occurredAt = Instant.parse("2026-10-05T12:00:00Z");

        OrderCreated event = new OrderCreated(
                eventId, occurredAt, "order-123", "customer-123", new BigDecimal("24.50"));

        assertEquals(eventId, event.eventId());
        assertEquals(1, event.eventVersion());
        assertEquals("OrderCreated", event.eventType());
        assertEquals(occurredAt, event.occurredAt());
        assertEquals("order-123", event.orderId());
        assertEquals("customer-123", event.customerId());
        assertEquals(new BigDecimal("24.50"), event.amount());
    }
}
