package com.aisre.platform.paymentservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;

@SpringBootTest(properties = {
        "spring.kafka.consumer.group-id=payment-service-integration",
        "payment.kafka.topic=payment-service-integration"
})
@EmbeddedKafka(
        kraft = true,
        partitions = 2,
        topics = PaymentKafkaConsumerIntegrationTest.TOPIC,
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@Import(PaymentKafkaConsumerIntegrationTest.ProcessorTestConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaymentKafkaConsumerIntegrationTest {

    static final String TOPIC = "payment-service-integration";
    private static final String GROUP_ID = "payment-service-integration";
    private static final int TEST_PARTITION = 0;
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(10);

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaListenerEndpointRegistry listenerRegistry;

    @Autowired
    private ControlledPaymentProcessor processor;

    private Admin admin;

    @BeforeEach
    void createAdminClient() {
        admin = AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString()));
    }

    @AfterEach
    void closeAdminClient() {
        admin.close();
    }

    @Test
    @Order(1)
    void retriesThenSucceedsAndSuppressesDuplicateDelivery() throws Exception {
        MessageListenerContainer container = listenerContainer();
        awaitCondition(
                () -> container.getAssignedPartitions() != null && container.getAssignedPartitions().size() == 2,
                "both topic partitions to be assigned");
        processor.failFirstAttempts(2);

        OrderCreated event = event();
        RecordMetadata firstDelivery = publish(event);

        assertTrue(processor.awaitSuccess(WAIT_TIMEOUT), "the retried event should eventually succeed");
        assertEquals(3, processor.attemptCount(), "two failures should be followed by one successful attempt");
        assertEquals(1, processor.successCount());
        awaitCondition(
                () -> committedOffset(TEST_PARTITION) == firstDelivery.offset() + 1,
                "the successful record offset to be committed");

        RecordMetadata duplicateDelivery = publish(event);

        awaitCondition(
                () -> committedOffset(TEST_PARTITION) == duplicateDelivery.offset() + 1,
                "the duplicate record to be acknowledged");
        assertEquals(3, processor.attemptCount(), "the duplicate must not invoke the processor again");
        assertEquals(1, processor.successCount(), "the in-memory guard is for this application lifetime only");
    }

    @Test
    @Order(2)
    void exhaustedRetriesStopContainerAndLeaveFailedOffsetUncommitted() throws Exception {
        MessageListenerContainer container = listenerContainer();
        awaitCondition(
                () -> container.getAssignedPartitions() != null && container.getAssignedPartitions().size() == 2,
                "both topic partitions to be assigned");
        processor.failEveryAttempt(3);

        OrderCreated event = event();
        RecordMetadata failedRecord = publish(event);

        assertTrue(processor.awaitAttempts(WAIT_TIMEOUT), "all configured attempts should occur");
        assertEquals(3, processor.attemptCount());
        assertEquals(0, processor.successCount());
        awaitCondition(() -> !container.isRunning(), "the configured stopping error handler to stop the listener");

        assertEquals(
                failedRecord.offset(),
                committedOffset(TEST_PARTITION),
                "RECORD acknowledgement must not commit the record whose listener failed");
    }

    private MessageListenerContainer listenerContainer() {
        return listenerRegistry.getListenerContainers().stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("Payment Kafka listener container was not registered"));
    }

    private RecordMetadata publish(OrderCreated event) throws Exception {
        return kafkaTemplate.send(new ProducerRecord<>(
                        TOPIC, TEST_PARTITION, event.orderId(), objectMapper.writeValueAsString(event)))
                .get(WAIT_TIMEOUT.toSeconds(), TimeUnit.SECONDS)
                .getRecordMetadata();
    }

    private long committedOffset(int partition) throws Exception {
        Map<TopicPartition, OffsetAndMetadata> offsets = admin.listConsumerGroupOffsets(GROUP_ID)
                .partitionsToOffsetAndMetadata()
                .get(WAIT_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        OffsetAndMetadata offset = offsets.get(new TopicPartition(TOPIC, partition));
        if (offset == null) {
            throw new AssertionError("No committed offset for " + TOPIC + " partition " + partition);
        }
        return offset.offset();
    }

    private static void awaitCondition(CheckedCondition condition, String description) throws Exception {
        long deadline = System.nanoTime() + WAIT_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.isTrue()) {
                return;
            }
            Thread.sleep(25);
        }
        assertTrue(condition.isTrue(), "Timed out waiting for " + description);
    }

    private static OrderCreated event() {
        return new OrderCreated(
                UUID.randomUUID(),
                1,
                "OrderCreated",
                Instant.now(),
                UUID.randomUUID().toString(),
                "integration-customer",
                new BigDecimal("24.50"));
    }

    @FunctionalInterface
    private interface CheckedCondition {

        boolean isTrue() throws Exception;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ProcessorTestConfiguration {

        @Bean
        @Primary
        ControlledPaymentProcessor controlledPaymentProcessor() {
            return new ControlledPaymentProcessor();
        }
    }

    static class ControlledPaymentProcessor implements PaymentProcessor {

        private final AtomicInteger attempts = new AtomicInteger();
        private final AtomicInteger successes = new AtomicInteger();
        private final AtomicInteger failuresRemaining = new AtomicInteger();
        private volatile boolean failAlways;
        private volatile CountDownLatch successSignal = new CountDownLatch(1);
        private volatile CountDownLatch attemptSignal = new CountDownLatch(0);

        void failFirstAttempts(int failures) {
            resetCounters();
            failuresRemaining.set(failures);
            failAlways = false;
        }

        void failEveryAttempt(int expectedAttempts) {
            resetCounters();
            failuresRemaining.set(0);
            failAlways = true;
            attemptSignal = new CountDownLatch(expectedAttempts);
        }

        int attemptCount() {
            return attempts.get();
        }

        int successCount() {
            return successes.get();
        }

        boolean awaitSuccess(Duration timeout) throws InterruptedException {
            return successSignal.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        boolean awaitAttempts(Duration timeout) throws InterruptedException {
            return attemptSignal.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public void process(OrderCreated event) {
            attempts.incrementAndGet();
            attemptSignal.countDown();

            if (failAlways || consumeFailure()) {
                throw new RetryablePaymentException("controlled integration-test failure");
            }

            successes.incrementAndGet();
            successSignal.countDown();
        }

        private boolean consumeFailure() {
            return failuresRemaining.getAndUpdate(remaining -> Math.max(remaining - 1, 0)) > 0;
        }

        private void resetCounters() {
            attempts.set(0);
            successes.set(0);
            successSignal = new CountDownLatch(1);
            attemptSignal = new CountDownLatch(0);
        }
    }
}
