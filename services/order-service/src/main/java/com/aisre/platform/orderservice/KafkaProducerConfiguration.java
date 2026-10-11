package com.aisre.platform.orderservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
public class KafkaProducerConfiguration {

    @Bean
    DefaultKafkaProducerFactoryCustomizer orderCreatedJsonSerializer(ObjectMapper objectMapper) {
        ObjectMapper kafkaObjectMapper = objectMapper.copy()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return producerFactory -> producerFactory.setValueSerializer(
                new JsonSerializer<>(kafkaObjectMapper));
    }
}
