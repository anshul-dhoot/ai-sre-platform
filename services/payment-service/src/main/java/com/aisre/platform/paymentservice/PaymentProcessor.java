package com.aisre.platform.paymentservice;

@FunctionalInterface
public interface PaymentProcessor {

    void process(OrderCreated event) throws RetryablePaymentException;
}
