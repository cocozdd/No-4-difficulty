package com.campusmarket.messaging;

import com.campusmarket.entity.Order;
import com.campusmarket.entity.OrderStatus;
import com.campusmarket.kafka.KafkaIntegrationTestBase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OrderEventPublisherIT extends KafkaIntegrationTestBase {

    @Autowired
    private OrderEventPublisher orderEventPublisher;

    private Consumer<String, OrderEvent> consumer;

    @BeforeEach
    void setUp() {
        consumer = createConsumer();
        consumer.subscribe(Collections.singletonList("order-events"));
        consumer.poll(Duration.ofMillis(100)); // ensure assignment
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    void publishOrderCreated_shouldProduceEvent() {
        Order order = new Order();
        order.setId(101L);
        order.setGoodsId(202L);
        order.setBuyerId(303L);
        order.setStatus(OrderStatus.PENDING_PAYMENT);

        orderEventPublisher.publishOrderCreated(order);

        ConsumerRecords<String, OrderEvent> records = pollForRecords();
        assertThat(records.count()).isGreaterThan(0);

        OrderEvent event = records.iterator().next().value();
        assertThat(event.getEventType()).isEqualTo(OrderEventType.ORDER_CREATED);
        assertThat(event.getOrderId()).isEqualTo(order.getId());
        assertThat(event.getGoodsId()).isEqualTo(order.getGoodsId());
        assertThat(event.getBuyerId()).isEqualTo(order.getBuyerId());
        assertThat(event.getEventId()).isNotBlank();
    }

    private ConsumerRecords<String, OrderEvent> pollForRecords() {
        ConsumerRecords<String, OrderEvent> records = ConsumerRecords.empty();
        int attempts = 0;
        while (records.isEmpty() && attempts < 5) {
            records = consumer.poll(Duration.ofSeconds(2));
            attempts++;
        }
        return records;
    }

    private Consumer<String, OrderEvent> createConsumer() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "order-events-it-" + System.nanoTime());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.campusmarket.messaging");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, OrderEvent.class.getName());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new KafkaConsumer<>(props);
    }
}
