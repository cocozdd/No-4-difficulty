package com.campusmarket.service.impl;

import com.campusmarket.messaging.GoodsEvent;
import com.campusmarket.messaging.GoodsEventType;
import com.campusmarket.service.GoodsMetricsService;
import com.campusmarket.service.HotGoodsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GoodsEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(GoodsEventProcessor.class);

    private final GoodsMetricsService goodsMetricsService;
    private final HotGoodsService hotGoodsService;

    public GoodsEventProcessor(GoodsMetricsService goodsMetricsService,
                               HotGoodsService hotGoodsService) {
        this.goodsMetricsService = goodsMetricsService;
        this.hotGoodsService = hotGoodsService;
    }

    public void handle(GoodsEvent event) {
        if (event == null) {
            return;
        }
        GoodsEventType type = event.getEventType();
        if (type == null) {
            log.debug("Ignore goods event without type: {}", event);
            return;
        }
        try {
            switch (type) {
                case GOODS_VIEWED:
                    goodsMetricsService.recordView(event.getGoodsId());
                    break;
                case GOODS_DELETED:
                    goodsMetricsService.removeMetrics(event.getGoodsId());
                    hotGoodsService.evictHotCache();
                    break;
                case GOODS_CREATED:
                case GOODS_UPDATED:
                case GOODS_REVIEWED:
                case GOODS_MARKED_SOLD:
                    hotGoodsService.evictHotCache();
                    break;
                default:
                    log.debug("Unhandled goods event type: {}", type);
            }
        } catch (RuntimeException ex) {
            log.warn("Failed to process goods event: {}", event, ex);
        }
    }
}
