package com.aisre.platform.orderservice;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    public Order createOrder(OrderRequest request) {
        Order order = new Order(
                UUID.randomUUID().toString(),
                request.customerId(),
                request.amount(),
                Instant.now());

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
