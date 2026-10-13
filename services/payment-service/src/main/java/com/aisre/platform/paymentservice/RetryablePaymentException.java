package com.aisre.platform.paymentservice;

public class RetryablePaymentException extends RuntimeException {

    public RetryablePaymentException(String message) {
        super(message);
    }

    public RetryablePaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
