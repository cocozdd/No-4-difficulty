package com.campusmarket.messaging;

import com.campusmarket.entity.Order;
import com.campusmarket.entity.OrderStatus;
import com.campusmarket.service.DistributedIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private final String orderTopic;
    private final DistributedIdGenerator idGenerator;

    public OrderEventPublisher(KafkaTemplate<String, OrderEvent> kafkaTemplate,
                               @Value("${app.kafka.order-topic:order-events}") String orderTopic,
                               DistributedIdGenerator idGenerator) {
        this.kafkaTemplate = kafkaTemplate;
        this.orderTopic = orderTopic;
        this.idGenerator = idGenerator;
    }

    public void publishOrderCreated(Order order) {
        if (order == null) {
            return;
        }
        OrderEvent event = new OrderEvent(
                idGenerator.nextIdAsString("order-event"),
                OrderEventType.ORDER_CREATED,
                order.getId(),
                order.getGoodsId(),
                order.getBuyerId(),
                statusAsString(order.getStatus()),
                null,
                LocalDateTime.now()
        );
        sendEvent(order.getId(), event);
    }

    public void publishOrderStatusChanged(Order order, OrderStatus previousStatus) {
        if (order == null) {
            return;
        }
        OrderEvent event = new OrderEvent(
                idGenerator.nextIdAsString("order-event"),
                OrderEventType.ORDER_STATUS_CHANGED,
                order.getId(),
                order.getGoodsId(),
                order.getBuyerId(),
                statusAsString(order.getStatus()),
                statusAsString(previousStatus),
                LocalDateTime.now()
        );
        sendEvent(order.getId(), event);
    }

    private void sendEvent(Long key, OrderEvent event) {
        try {
            kafkaTemplate.send(orderTopic, key == null ? null : key.toString(), event);
            log.debug("Published order event: {}", event);
        } catch (Exception ex) {
            log.warn("Failed to publish order event {}", event, ex);
        }
    }

    private String statusAsString(OrderStatus status) {
        return status == null ? null : status.name();
    }
}
