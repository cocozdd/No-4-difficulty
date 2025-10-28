package com.campusmarket.service;

import com.campusmarket.dto.FlashSaleItemCreateRequest;
import com.campusmarket.dto.FlashSaleItemResponse;

import java.util.List;

public interface FlashSaleService {

    FlashSaleItemResponse createFlashSaleItem(FlashSaleItemCreateRequest request);

    List<FlashSaleItemResponse> listUpcomingAndActiveItems();

    /**
     * Attempt to purchase a flash sale item for the given user.
     *
     * @return order id when success, otherwise null
     */
    Long attemptPurchase(Long userId, Long flashSaleItemId);
}
