package com.aisre.platform.paymentservice;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedListener {

    private final PaymentService paymentService;

    public OrderCreatedListener(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @KafkaListener(topics = "${payment.kafka.topic:orders.events}")
    public void onOrderCreated(OrderCreated event) {
        paymentService.process(event);
    }
}
