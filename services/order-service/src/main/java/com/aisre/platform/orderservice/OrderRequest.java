package com.aisre.platform.orderservice;

import java.math.BigDecimal;

public record OrderRequest(String customerId, BigDecimal amount) {
}
