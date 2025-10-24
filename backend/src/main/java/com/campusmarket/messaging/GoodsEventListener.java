package com.campusmarket.messaging;

import com.campusmarket.service.impl.GoodsEventProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class GoodsEventListener {

    private static final Logger log = LoggerFactory.getLogger(GoodsEventListener.class);

    private final GoodsEventProcessor goodsEventProcessor;

    public GoodsEventListener(GoodsEventProcessor goodsEventProcessor) {
        this.goodsEventProcessor = goodsEventProcessor;
    }

    @KafkaListener(
            topics = "${app.kafka.goods-topic:goods-events}",
            groupId = "${spring.kafka.consumer.group-id}-goods"
    )
    public void handleGoodsEvent(GoodsEvent event) {
        log.info("Received goods event: type={}, goodsId={}, sellerId={}, actor={}, note={}",
                event.getEventType(),
                event.getGoodsId(),
                event.getSellerId(),
                event.getActorId(),
                event.getNote());
        goodsEventProcessor.handle(event);
    }
}
