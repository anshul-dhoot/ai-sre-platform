package com.aisre.platform.paymentservice;

import org.springframework.stereotype.Component;

@Component
public class SimulatedPaymentProcessor implements PaymentProcessor {

    @Override
    public void process(OrderCreated event) {
        // Simulate a successful payment without contacting a gateway or storing payment data.
    }
}
