package com.campusmarket.service;

import com.campusmarket.dto.GoodsFilterRequest;
import com.campusmarket.dto.GoodsResponse;
import com.campusmarket.dto.GoodsUpdateRequest;
import com.campusmarket.entity.Goods;
import com.campusmarket.entity.GoodsStatus;

import java.util.List;

public interface GoodsService {
    List<GoodsResponse> listGoods(GoodsFilterRequest request);
    GoodsResponse getGoods(Long id, Long viewerId, boolean adminView);
    Goods getGoodsEntity(Long id);
    Goods createGoods(Goods goods);
    GoodsResponse updateGoods(Long id, GoodsUpdateRequest request, Long sellerId);
    void deleteGoods(Long id, Long sellerId);
    List<GoodsResponse> listGoodsBySeller(Long sellerId);
    List<GoodsResponse> listGoodsByStatus(GoodsStatus status);
    GoodsResponse reviewGoods(Long id, GoodsStatus status);
    void markSold(Long goodsId, boolean sold);
    void seedInitialGoods();
}
