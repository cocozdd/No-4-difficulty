package com.campusmarket.service;

import com.campusmarket.dto.KafkaTestRequest;
import com.campusmarket.dto.KafkaTestResponse;
import com.campusmarket.messaging.OrderEvent;
import com.campusmarket.messaging.OrderEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class KafkaDiagnosticsService {

    private static final Logger log = LoggerFactory.getLogger(KafkaDiagnosticsService.class);

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private final String orderTopic;
    private final DistributedIdGenerator idGenerator;

    public KafkaDiagnosticsService(KafkaTemplate<String, OrderEvent> kafkaTemplate,
                                   @Value("${app.kafka.order-topic:order-events}") String orderTopic,
                                   DistributedIdGenerator idGenerator) {
        this.kafkaTemplate = kafkaTemplate;
        this.orderTopic = orderTopic;
        this.idGenerator = idGenerator;
    }

    public KafkaTestResponse publishOrderEvent(KafkaTestRequest request) {
        LocalDateTime dispatchedAt = LocalDateTime.now();
        String key = resolveKey(request);

        OrderEvent event = new OrderEvent(
                OrderEventType.TEST_EVENT,
                request.getOrderId(),
                request.getGoodsId(),
                request.getBuyerId(),
                Optional.ofNullable(request.getCurrentStatus()).orElse("TEST"),
                request.getPreviousStatus(),
                dispatchedAt
        );
        event.setEventId(idGenerator.nextIdAsString("order-event"));
        event.setNote(Optional.ofNullable(request.getMessage())
                .filter(msg -> !msg.isBlank())
                .orElse("Kafka diagnostics event dispatched"));

        CompletableFuture<SendResult<String, OrderEvent>> future = kafkaTemplate.send(orderTopic, key, event)
                .completable();

        try {
            SendResult<String, OrderEvent> result = future.get(5, TimeUnit.SECONDS);
            log.info("Kafka diagnostics event published: topic={}, partition={}, offset={}, note={}",
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset(),
                    event.getNote());
            return new KafkaTestResponse(orderTopic, key, dispatchedAt, event.getNote());
        } catch (TimeoutException timeoutException) {
            future.cancel(true);
            log.warn("Kafka diagnostics event timed out after 5 seconds: key={}, note={}",
                    key, event.getNote());
            throw new IllegalStateException("Kafka diagnostics send timed out, please retry.", timeoutException);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Kafka diagnostics send interrupted.", interruptedException);
        } catch (ExecutionException executionException) {
            throw new IllegalStateException("Kafka diagnostics send failed.", executionException.getCause());
        }
    }

    private String resolveKey(KafkaTestRequest request) {
        if (request.getKey() != null && !request.getKey().isBlank()) {
            return request.getKey();
        }
        if (Objects.nonNull(request.getOrderId())) {
            return String.valueOf(request.getOrderId());
        }
        return "diag-" + UUID.randomUUID();
    }
}
