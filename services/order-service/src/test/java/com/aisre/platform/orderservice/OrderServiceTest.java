package com.aisre.platform.orderservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class OrderServiceTest {

    @Test
    void createsOrderWithGeneratedIdAndTimestamp() {
        OrderService service = new OrderService();
        Instant beforeCreation = Instant.now();

        Order order = service.createOrder(new OrderRequest("customer-123", new BigDecimal("24.50")));

        Instant afterCreation = Instant.now();
        assertNotNull(order.orderId());
        assertEquals("customer-123", order.customerId());
        assertEquals(new BigDecimal("24.50"), order.amount());
        assertNotNull(order.createdAt());
        assertTrue(!order.createdAt().isBefore(beforeCreation) && !order.createdAt().isAfter(afterCreation));
    }
}
