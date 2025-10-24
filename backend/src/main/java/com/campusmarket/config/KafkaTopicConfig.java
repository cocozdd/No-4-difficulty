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

    @Value("${app.kafka.goods-topic:goods-events}")
    private String goodsTopicName;

    @Value("${app.kafka.chat-topic:chat-events}")
    private String chatTopicName;

    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name(orderTopicName)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic goodsEventsTopic() {
        return TopicBuilder.name(goodsTopicName)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic goodsEventsDltTopic() {
        return TopicBuilder.name(goodsTopicName + ".DLT")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic chatEventsTopic() {
        return TopicBuilder.name(chatTopicName)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
