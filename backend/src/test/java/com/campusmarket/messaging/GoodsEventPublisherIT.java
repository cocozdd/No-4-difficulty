package com.campusmarket.messaging;

import com.campusmarket.kafka.KafkaIntegrationTestBase;
import com.campusmarket.service.GoodsCacheService;
import com.campusmarket.service.GoodsMetricsService;
import com.campusmarket.service.HotGoodsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest
class GoodsEventPublisherIT extends KafkaIntegrationTestBase {

    @Autowired
    private GoodsEventPublisher goodsEventPublisher;

    @MockBean
    private GoodsMetricsService goodsMetricsService;

    @MockBean
    private HotGoodsService hotGoodsService;

    @MockBean
    private GoodsCacheService goodsCacheService;

    @Test
    void publishGoodsViewed_shouldRecordView() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(goodsMetricsService).recordView(1001L);

        goodsEventPublisher.publishGoodsViewed(1001L, 2001L, 3001L);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        verify(goodsMetricsService).recordView(1001L);
        verifyNoMoreInteractions(hotGoodsService);
        verifyNoInteractions(goodsCacheService);
    }

    @Test
    void publishGoodsDeleted_shouldRemoveMetricsAndEvictCache() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(goodsMetricsService).removeMetrics(1002L);

        goodsEventPublisher.publishGoodsDeleted(buildGoodsEventEntity(1002L, 2002L), 4002L);

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        verify(goodsMetricsService).removeMetrics(1002L);
        verify(goodsCacheService).evictAllForGoods(1002L);
        verify(hotGoodsService).evictHotCache();
    }

    @Test
    void publishGoodsCreated_shouldEvictCachesAndHotCache() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(goodsCacheService).evictAllForGoods(1003L);

        goodsEventPublisher.publishGoodsCreated(buildGoodsEventEntity(1003L, 2003L));

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        verify(goodsCacheService).evictAllForGoods(1003L);
        verify(hotGoodsService).evictHotCache();
        verifyNoMoreInteractions(goodsMetricsService);
    }

    private com.campusmarket.entity.Goods buildGoodsEventEntity(Long goodsId, Long sellerId) {
        com.campusmarket.entity.Goods goods = new com.campusmarket.entity.Goods();
        goods.setId(goodsId);
        goods.setSellerId(sellerId);
        goods.setStatus("APPROVED");
        goods.setQuantity(10);
        return goods;
    }
}
