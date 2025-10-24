package com.campusmarket.service;

/**
 * Centralised cache eviction for goods-related Redis keys.
 */
public interface GoodsCacheService {

    /**
     * Remove the cached goods detail (including null markers) for the given goods id.
     *
     * @param goodsId the goods identifier, ignored when null
     */
    void evictGoodsDetail(Long goodsId);

    /**
     * Remove all cached goods list variants.
     */
    void evictGoodsLists();

    /**
     * Convenience method that removes both detail cache and list caches for the goods id.
     *
     * @param goodsId the goods identifier, ignored when null
     */
    default void evictAllForGoods(Long goodsId) {
        evictGoodsDetail(goodsId);
        evictGoodsLists();
    }
}
