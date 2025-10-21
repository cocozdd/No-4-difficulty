package com.campusmarket.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${app.kafka.order-topic:order-events}")
    private String orderTopicName;

    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name(orderTopicName)
                .partitions(3)
                .replicas(1)
                .build();
    }
}

