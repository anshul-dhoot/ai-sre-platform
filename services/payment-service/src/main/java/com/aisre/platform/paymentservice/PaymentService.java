package com.aisre.platform.paymentservice;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final int SUPPORTED_EVENT_VERSION = 1;
    private static final String SUPPORTED_EVENT_TYPE = "OrderCreated";

    private final PaymentProcessor paymentProcessor;
    private final int maxAttempts;
    private final long retryDelayMillis;
    private final Set<String> processedOrderIds = ConcurrentHashMap.newKeySet();

    public PaymentService(
            PaymentProcessor paymentProcessor,
            @Value("${payment.processing.max-attempts:3}") int maxAttempts,
            @Value("${payment.processing.retry-delay:250ms}") Duration retryDelay) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
        this.paymentProcessor = paymentProcessor;
        this.maxAttempts = maxAttempts;
        this.retryDelayMillis = retryDelay.toMillis();
    }

    public void process(OrderCreated event) {
        validate(event);

        if (processedOrderIds.contains(event.orderId())) {
            log.atInfo()
                    .addKeyValue("event", "payment_duplicate_detected")
                    .addKeyValue("eventId", event.eventId())
                    .addKeyValue("orderId", event.orderId())
                    .log("Order already processed");
            return;
        }

        processWithRetries(event);
        processedOrderIds.add(event.orderId());

        log.atInfo()
                .addKeyValue("event", "payment_succeeded")
                .addKeyValue("eventId", event.eventId())
                .addKeyValue("orderId", event.orderId())
                .log("Payment processed");
    }

    private void processWithRetries(OrderCreated event) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                paymentProcessor.process(event);
                return;
            } catch (RetryablePaymentException exception) {
                if (attempt == maxAttempts) {
                    logProcessingFailure(event, attempt, exception, "Payment processing retries exhausted");
                    throw exception;
                }
                log.atWarn()
                        .addKeyValue("event", "payment_processing_retry")
                        .addKeyValue("eventId", event.eventId())
                        .addKeyValue("orderId", event.orderId())
                        .addKeyValue("attempt", attempt)
                        .log("Retrying payment processing");
                pauseBeforeRetry();
            } catch (RuntimeException exception) {
                logProcessingFailure(event, attempt, exception, "Payment processing failed permanently");
                throw exception;
            }
        }
    }

    private static void logProcessingFailure(
            OrderCreated event, int attempt, RuntimeException exception, String message) {
        log.atError()
                .addKeyValue("event", "payment_processing_failed")
                .addKeyValue("eventId", event.eventId())
                .addKeyValue("orderId", event.orderId())
                .addKeyValue("attempts", attempt)
                .setCause(exception)
                .log(message);
    }

    private void pauseBeforeRetry() {
        try {
            Thread.sleep(retryDelayMillis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new RetryablePaymentException("Payment retry interrupted", interrupted);
        }
    }

    private static void validate(OrderCreated event) {
        if (event == null) {
            throw new InvalidOrderCreatedException("OrderCreated event is required");
        }
        if (event.eventId() == null) {
            throw new InvalidOrderCreatedException("eventId is required");
        }
        if (event.eventVersion() != SUPPORTED_EVENT_VERSION) {
            throw new InvalidOrderCreatedException("Unsupported eventVersion");
        }
        if (!SUPPORTED_EVENT_TYPE.equals(event.eventType())) {
            throw new InvalidOrderCreatedException("Unsupported eventType");
        }
        if (event.occurredAt() == null) {
            throw new InvalidOrderCreatedException("occurredAt is required");
        }
        requireUuid(event.orderId(), "orderId");
        if (event.customerId() == null || event.customerId().isBlank()) {
            throw new InvalidOrderCreatedException("customerId is required");
        }
        BigDecimal amount = event.amount();
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidOrderCreatedException("amount must be greater than zero");
        }
    }

    private static void requireUuid(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidOrderCreatedException(field + " is required");
        }
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new InvalidOrderCreatedException(field + " must be a UUID");
        }
    }
}
