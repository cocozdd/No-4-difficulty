package com.campusmarket.messaging;

import com.campusmarket.entity.Goods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class GoodsEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(GoodsEventPublisher.class);

    private final KafkaTemplate<String, GoodsEvent> kafkaTemplate;
    private final String goodsTopic;

    public GoodsEventPublisher(KafkaTemplate<String, GoodsEvent> kafkaTemplate,
                               @Value("${app.kafka.goods-topic:goods-events}") String goodsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.goodsTopic = goodsTopic;
    }

    public void publishGoodsCreated(Goods goods) {
        sendEvent(goods, null, GoodsEventType.GOODS_CREATED, "Goods created");
    }

    public void publishGoodsUpdated(Goods goods, Long actorId, String note) {
        sendEvent(goods, actorId, GoodsEventType.GOODS_UPDATED, note);
    }

    public void publishGoodsDeleted(Goods goods, Long actorId) {
        sendEvent(goods, actorId, GoodsEventType.GOODS_DELETED, "Goods deleted");
    }

    public void publishGoodsReviewed(Goods goods, Long reviewerId, String note) {
        sendEvent(goods, reviewerId, GoodsEventType.GOODS_REVIEWED, note);
    }

    public void publishGoodsMarkedSold(Goods goods, Long actorId) {
        sendEvent(goods, actorId, GoodsEventType.GOODS_MARKED_SOLD, "Goods marked sold=" + goods.getSold());
    }

    public void publishGoodsViewed(Long goodsId, Long sellerId, Long viewerId) {
        GoodsEvent event = new GoodsEvent(
                GoodsEventType.GOODS_VIEWED,
                goodsId,
                sellerId,
                viewerId,
                null,
                null,
                "Goods viewed",
                LocalDateTime.now()
        );
        send(goodsId, event);
    }

    private void sendEvent(Goods goods, Long actorId, GoodsEventType type, String note) {
        if (goods == null) {
            return;
        }
        GoodsEvent event = new GoodsEvent(
                type,
                goods.getId(),
                goods.getSellerId(),
                actorId,
                goods.getStatus(),
                goods.getQuantity(),
                Optional.ofNullable(note).orElse(type.name()),
                LocalDateTime.now()
        );
        send(goods.getId(), event);
    }

    private void send(Long key, GoodsEvent event) {
        String keyString = key == null ? null : key.toString();
        kafkaTemplate.send(goodsTopic, keyString, event)
                .addCallback(result -> {
                            if (log.isDebugEnabled() && result != null) {
                                log.debug("Published goods event: type={}, goodsId={}, partition={}, offset={}",
                                        event.getEventType(),
                                        event.getGoodsId(),
                                        result.getRecordMetadata().partition(),
                                        result.getRecordMetadata().offset());
                            }
                        },
                        throwable -> log.warn("Failed to publish goods event: {}", event, throwable));
    }
}
