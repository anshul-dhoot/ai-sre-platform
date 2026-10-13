package com.aisre.platform.paymentservice;

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
}
