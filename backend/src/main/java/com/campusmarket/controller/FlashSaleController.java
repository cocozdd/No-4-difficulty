package com.campusmarket.controller;

import com.campusmarket.dto.FlashSaleItemCreateRequest;
import com.campusmarket.dto.FlashSaleItemResponse;
import com.campusmarket.dto.FlashSalePurchaseRequest;
import com.campusmarket.entity.User;
import com.campusmarket.service.FlashSaleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/flash-sale")
public class FlashSaleController {

    private final FlashSaleService flashSaleService;

    public FlashSaleController(FlashSaleService flashSaleService) {
        this.flashSaleService = flashSaleService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/items")
    public FlashSaleItemResponse createFlashSaleItem(@RequestBody @Validated FlashSaleItemCreateRequest request) {
        return flashSaleService.createFlashSaleItem(request);
    }

    @GetMapping("/items")
    public List<FlashSaleItemResponse> listItems() {
        return flashSaleService.listUpcomingAndActiveItems();
    }

    @PostMapping("/purchase")
    public ResponseEntity<Map<String, Object>> purchase(@AuthenticationPrincipal User currentUser,
                                                        @RequestBody @Validated FlashSalePurchaseRequest request) {
        Long orderId = flashSaleService.attemptPurchase(currentUser.getId(), request.getFlashSaleItemId());
        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        result.put("message", "抢购成功，正在创建订单");
        return ResponseEntity.ok(result);
    }
}
