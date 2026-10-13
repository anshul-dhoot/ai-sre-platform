package com.aisre.platform.paymentservice;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JsonDeserializer;

class PaymentServiceTest {

    private static final String ORDER_ID = "550e8400-e29b-41d4-a716-446655440001";

    @Test
    void rejectsInvalidEventBeforeProcessing() {
        PaymentProcessor processor = org.mockito.Mockito.mock(PaymentProcessor.class);
        PaymentService service = new PaymentService(processor, 3, Duration.ZERO);
        OrderCreated valid = event();
        List<OrderCreated> invalidEvents = List.of(
                new OrderCreated(valid.eventId(), 2, valid.eventType(), valid.occurredAt(), valid.orderId(), valid.customerId(), valid.amount()),
                new OrderCreated(valid.eventId(), 1, "OtherEvent", valid.occurredAt(), valid.orderId(), valid.customerId(), valid.amount()),
                new OrderCreated(null, 1, valid.eventType(), valid.occurredAt(), valid.orderId(), valid.customerId(), valid.amount()),
                new OrderCreated(valid.eventId(), 1, valid.eventType(), null, valid.orderId(), valid.customerId(), valid.amount()),
                new OrderCreated(valid.eventId(), 1, valid.eventType(), valid.occurredAt(), "not-a-uuid", valid.customerId(), valid.amount()),
                new OrderCreated(valid.eventId(), 1, valid.eventType(), valid.occurredAt(), valid.orderId(), " ", valid.amount()),
                new OrderCreated(valid.eventId(), 1, valid.eventType(), valid.occurredAt(), valid.orderId(), valid.customerId(), null),
                new OrderCreated(valid.eventId(), 1, valid.eventType(), valid.occurredAt(), valid.orderId(), valid.customerId(), BigDecimal.ZERO));

        invalidEvents.forEach(invalid -> assertThrows(
                InvalidOrderCreatedException.class, () -> service.process(invalid)));
        assertThrows(InvalidOrderCreatedException.class, () -> service.process(null));

        verify(processor, never()).process(any());
    }

    @Test
    void processesValidEventSuccessfully() {
        PaymentProcessor processor = org.mockito.Mockito.mock(PaymentProcessor.class);
        PaymentService service = new PaymentService(processor, 3, Duration.ZERO);
        OrderCreated event = event();

        service.process(event);

        verify(processor).process(event);
    }

    @Test
    void detectsDuplicateByOrderIdAfterSuccess() {
        PaymentProcessor processor = org.mockito.Mockito.mock(PaymentProcessor.class);
        PaymentService service = new PaymentService(processor, 3, Duration.ZERO);

        service.process(event());
        service.process(new OrderCreated(
                UUID.randomUUID(), 1, "OrderCreated", Instant.parse("2026-10-11T10:36:13.627471700Z"),
                ORDER_ID, "customer-123", new BigDecimal("24.50")));

        verify(processor, times(1)).process(any());
    }

    @Test
    void retriesRetryableFailureAndDoesNotMarkOrderProcessed() {
        PaymentProcessor processor = org.mockito.Mockito.mock(PaymentProcessor.class);
        OrderCreated event = event();
        doThrow(new RetryablePaymentException("temporary processing failure"))
                .when(processor).process(event);
        PaymentService service = new PaymentService(processor, 3, Duration.ZERO);

        assertThrows(RetryablePaymentException.class, () -> service.process(event));
        assertThrows(RetryablePaymentException.class, () -> service.process(event));

        verify(processor, times(6)).process(event);
    }

    @Test
    void deserializesProducerJsonIncludingIso8601OccurredAt() {
        String json = """
                {"eventId":"550e8400-e29b-41d4-a716-446655440000","eventVersion":1,
                 "eventType":"OrderCreated","occurredAt":"2026-10-11T10:36:13.627471700Z",
                 "orderId":"550e8400-e29b-41d4-a716-446655440001","customerId":"customer-123","amount":24.50}
                """;
        JsonDeserializer<OrderCreated> deserializer = new JsonDeserializer<>(OrderCreated.class, false);
        Headers headers = new RecordHeaders()
                .add("__TypeId__", "com.aisre.platform.orderservice.OrderCreated".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        OrderCreated event = deserializer.deserialize(
                "orders.events", headers, json.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertEquals(Instant.parse("2026-10-11T10:36:13.627471700Z"), event.occurredAt());
        assertEquals(new BigDecimal("24.50"), event.amount());
    }

    private static OrderCreated event() {
        return new OrderCreated(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                1,
                "OrderCreated",
                Instant.parse("2026-10-11T10:36:13.627471700Z"),
                ORDER_ID,
                "customer-123",
                new BigDecimal("24.50"));
    }
}
