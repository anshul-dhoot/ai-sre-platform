package com.aisre.platform.paymentservice;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonContainerStoppingErrorHandler;
import org.springframework.kafka.listener.CommonErrorHandler;

@Configuration
public class KafkaConsumerConfiguration {

    @Bean
    CommonErrorHandler stopConsumerOnUnprocessedRecord() {
        return new CommonContainerStoppingErrorHandler();
    }
}
