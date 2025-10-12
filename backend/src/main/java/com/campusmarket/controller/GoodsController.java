package com.campusmarket.controller;

import com.campusmarket.dto.GoodsCreateRequest;
import com.campusmarket.dto.GoodsFilterRequest;
import com.campusmarket.dto.GoodsResponse;
import com.campusmarket.dto.GoodsReviewRequest;
import com.campusmarket.dto.GoodsUpdateRequest;
import com.campusmarket.entity.Goods;
import com.campusmarket.entity.User;
import com.campusmarket.entity.GoodsStatus;
import com.campusmarket.service.GoodsService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/goods")
public class GoodsController {

    private final GoodsService goodsService;

    public GoodsController(GoodsService goodsService) {
        this.goodsService = goodsService;
    }

    @GetMapping
    public List<GoodsResponse> listGoods(@RequestParam(required = false) String category,
                                         @RequestParam(required = false) BigDecimal minPrice,
                                         @RequestParam(required = false) BigDecimal maxPrice,
                                         @RequestParam(required = false) String keyword) {
        GoodsFilterRequest request = new GoodsFilterRequest();
        request.setCategory(category);
        request.setMinPrice(minPrice);
        request.setMaxPrice(maxPrice);
        request.setKeyword(keyword);
        return goodsService.listGoods(request);
    }

    @GetMapping("/{id}")
    public GoodsResponse getGoods(@PathVariable Long id,
                                  @AuthenticationPrincipal User user) {
        Long viewerId = user != null ? user.getId() : null;
        boolean adminView = user != null && "ADMIN".equalsIgnoreCase(user.getRole());
        return goodsService.getGoods(id, viewerId, adminView);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
    public List<GoodsResponse> listMyGoods(@AuthenticationPrincipal User user) {
        return goodsService.listGoodsBySeller(user.getId());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
    public GoodsResponse createGoods(@RequestBody @Validated GoodsCreateRequest request,
                                     @AuthenticationPrincipal User user) {
        Goods goods = new Goods();
        goods.setTitle(request.getTitle());
        goods.setDescription(request.getDescription());
        goods.setCategory(request.getCategory());
        goods.setPrice(request.getPrice());
        goods.setCoverImageUrl(request.getCoverImageUrl());
        goods.setSellerId(user.getId());
        Goods saved = goodsService.createGoods(goods);
        boolean adminView = "ADMIN".equalsIgnoreCase(user.getRole());
        return goodsService.getGoods(saved.getId(), user.getId(), adminView);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
    public GoodsResponse updateGoods(@PathVariable Long id,
                                     @RequestBody @Validated GoodsUpdateRequest request,
                                     @AuthenticationPrincipal User user) {
        return goodsService.updateGoods(id, request, user.getId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
    public void deleteGoods(@PathVariable Long id,
                            @AuthenticationPrincipal User user) {
        goodsService.deleteGoods(id, user.getId());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public List<GoodsResponse> listPendingGoods() {
        return goodsService.listGoodsByStatus(GoodsStatus.PENDING_REVIEW);
    }

    @PutMapping("/{id}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public GoodsResponse reviewGoods(@PathVariable Long id,
                                     @RequestBody @Validated GoodsReviewRequest request) {
        return goodsService.reviewGoods(id, request.getStatus());
    }
}
