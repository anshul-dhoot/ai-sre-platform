package com.aisre.platform.paymentservice;

public class InvalidOrderCreatedException extends RuntimeException {

    public InvalidOrderCreatedException(String message) {
        super(message);
    }
}
