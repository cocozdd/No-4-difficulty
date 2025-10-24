package com.campusmarket.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    @KafkaListener(topics = "${app.kafka.order-topic:order-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderEvent(OrderEvent event) {
        log.info("Received order event: type={}, orderId={}, status={}, note={}",
                event.getEventType(), event.getOrderId(), event.getCurrentStatus(), event.getNote());
    }
}
