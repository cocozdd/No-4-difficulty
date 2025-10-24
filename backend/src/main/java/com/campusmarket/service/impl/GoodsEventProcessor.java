package com.campusmarket.service.impl;

import com.campusmarket.messaging.GoodsEvent;
import com.campusmarket.messaging.GoodsEventType;
import com.campusmarket.service.GoodsCacheService;
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
    private final GoodsCacheService goodsCacheService;

    public GoodsEventProcessor(GoodsMetricsService goodsMetricsService,
                               HotGoodsService hotGoodsService,
                               GoodsCacheService goodsCacheService) {
        this.goodsMetricsService = goodsMetricsService;
        this.hotGoodsService = hotGoodsService;
        this.goodsCacheService = goodsCacheService;
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
            boolean evictCaches = false;
            boolean dropMetrics = false;
            switch (type) {
                case GOODS_VIEWED:
                    goodsMetricsService.recordView(event.getGoodsId());
                    break;
                case GOODS_DELETED:
                    dropMetrics = true;
                    evictCaches = true;
                    break;
                case GOODS_MARKED_SOLD:
                    dropMetrics = true;
                    evictCaches = true;
                    break;
                case GOODS_CREATED:
                case GOODS_UPDATED:
                case GOODS_REVIEWED:
                    evictCaches = true;
                    break;
                default:
                    log.debug("Unhandled goods event type: {}", type);
            }
            if (dropMetrics) {
                goodsMetricsService.removeMetrics(event.getGoodsId());
            }
            if (evictCaches) {
                goodsCacheService.evictAllForGoods(event.getGoodsId());
                hotGoodsService.evictHotCache();
            }
        } catch (RuntimeException ex) {
            log.warn("Failed to process goods event: {}", event, ex);
        }
    }
}
