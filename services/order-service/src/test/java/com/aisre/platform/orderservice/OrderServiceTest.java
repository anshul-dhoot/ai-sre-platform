package com.aisre.platform.orderservice;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

class OrderServiceTest {

    @Test
    void createsOrderWithGeneratedIdAndTimestamp() {
        KafkaTemplate<String, OrderCreated> kafkaTemplate = mock(KafkaTemplate.class);
        OrderService service = new OrderService(kafkaTemplate);
        Instant beforeCreation = Instant.now();

        Order order = service.createOrder(new OrderRequest("customer-123", new BigDecimal("24.50")));

        Instant afterCreation = Instant.now();
        assertNotNull(order.orderId());
        assertDoesNotThrow(() -> UUID.fromString(order.orderId()));
        assertEquals("customer-123", order.customerId());
        assertEquals(new BigDecimal("24.50"), order.amount());
        assertNotNull(order.createdAt());
        assertTrue(!order.createdAt().isBefore(beforeCreation) && !order.createdAt().isAfter(afterCreation));

        ArgumentCaptor<OrderCreated> eventCaptor = ArgumentCaptor.forClass(OrderCreated.class);
        verify(kafkaTemplate).send(eq("orders.events"), eq(order.orderId()), eventCaptor.capture());

        OrderCreated event = eventCaptor.getValue();
        assertNotNull(event.eventId());
        assertEquals(1, event.eventVersion());
        assertEquals("OrderCreated", event.eventType());
        assertEquals(order.createdAt(), event.occurredAt());
        assertEquals(order.orderId(), event.orderId());
        assertEquals(order.customerId(), event.customerId());
        assertEquals(order.amount(), event.amount());
    }
}
