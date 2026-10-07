package com.aisre.platform.orderservice;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final String ORDERS_TOPIC = "orders.events";

    private final KafkaTemplate<String, OrderCreated> kafkaTemplate;

    public OrderService(KafkaTemplate<String, OrderCreated> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public Order createOrder(OrderRequest request) {
        Order order = new Order(
                UUID.randomUUID().toString(),
                request.customerId(),
                request.amount(),
                Instant.now());

        OrderCreated event = new OrderCreated(
                UUID.randomUUID(),
                order.createdAt(),
                order.orderId(),
                order.customerId(),
                order.amount());

        kafkaTemplate.send(ORDERS_TOPIC, order.orderId(), event);

        log.atInfo()
                .addKeyValue("event", "order_created")
                .addKeyValue("orderId", order.orderId())
                .addKeyValue("customerId", order.customerId())
                .addKeyValue("amount", order.amount())
                .addKeyValue("createdAt", order.createdAt())
                .log("Order created");

        return order;
    }
}
