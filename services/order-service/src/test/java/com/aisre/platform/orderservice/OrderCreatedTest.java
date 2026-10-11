package com.aisre.platform.orderservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

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

    @Test
    void kafkaSerializerWritesOccurredAtAsIso8601UtcString() {
        Instant occurredAt = Instant.parse("2026-10-11T10:36:13.627471700Z");
        OrderCreated event = new OrderCreated(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), occurredAt,
                "order-123", "customer-123", new BigDecimal("24.50"));
        DefaultKafkaProducerFactory<String, OrderCreated> producerFactory = mock(DefaultKafkaProducerFactory.class);

        new KafkaProducerConfiguration().orderCreatedJsonSerializer(new ObjectMapper().findAndRegisterModules())
                .customize(producerFactory);

        ArgumentCaptor<JsonSerializer<OrderCreated>> serializerCaptor = ArgumentCaptor.forClass(JsonSerializer.class);
        verify(producerFactory).setValueSerializer(serializerCaptor.capture());
        String json = new String(serializerCaptor.getValue().serialize("orders.events", event),
                java.nio.charset.StandardCharsets.UTF_8);

        assertTrue(json.contains("\"occurredAt\":\"2026-10-11T10:36:13.627471700Z\""), json);
    }
}
