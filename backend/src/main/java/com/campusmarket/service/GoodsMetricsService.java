package com.campusmarket.service;

import java.util.Collection;
import java.util.Map;

public interface GoodsMetricsService {

    void recordView(Long goodsId);

    void recordOrder(Long goodsId);

    void recordCartAddition(Long goodsId);

    Map<Long, Long> getOrderCounts(Collection<Long> goodsIds);

    Map<Long, Long> getCartCounts(Collection<Long> goodsIds);

    Map<Long, Long> getViewCounts(Collection<Long> goodsIds);

    void removeMetrics(Long goodsId);
}
