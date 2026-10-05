package com.aisre.platform.orderservice;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderCreated(
        UUID eventId,
        int eventVersion,
        String eventType,
        Instant occurredAt,
        String orderId,
        String customerId,
        BigDecimal amount) {

    public static final int CURRENT_VERSION = 1;
    public static final String EVENT_TYPE = "OrderCreated";

    public OrderCreated(
            UUID eventId,
            Instant occurredAt,
            String orderId,
            String customerId,
            BigDecimal amount) {
        this(eventId, CURRENT_VERSION, EVENT_TYPE, occurredAt, orderId, customerId, amount);
    }
}
