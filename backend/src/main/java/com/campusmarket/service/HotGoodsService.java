package com.campusmarket.service;

import com.campusmarket.dto.HotGoodsItemResponse;

import java.util.List;

public interface HotGoodsService {

    List<HotGoodsItemResponse> getTopHotGoods(int limit);

    void evictHotCache();

    List<HotGoodsItemResponse> getRanking(String metric, int limit);
}
