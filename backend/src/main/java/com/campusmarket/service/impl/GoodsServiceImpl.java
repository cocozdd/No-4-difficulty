package com.campusmarket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusmarket.dto.GoodsFilterRequest;
import com.campusmarket.dto.GoodsResponse;
import com.campusmarket.dto.GoodsUpdateRequest;
import com.campusmarket.entity.Goods;
import com.campusmarket.entity.GoodsStatus;
import com.campusmarket.mapper.GoodsMapper;
import com.campusmarket.service.GoodsService;
import com.campusmarket.service.UserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GoodsServiceImpl implements GoodsService {

    private final GoodsMapper goodsMapper;
    private final UserService userService;

    public GoodsServiceImpl(GoodsMapper goodsMapper, UserService userService) {
        this.goodsMapper = goodsMapper;
        this.userService = userService;
    }

    @PostConstruct
    @Override
    public void seedInitialGoods() {
        if (goodsMapper.selectCount(null) > 0) {
            return;
        }
        Goods laptop = new Goods();
        laptop.setTitle("Second-hand Laptop");
        laptop.setDescription("95% new ultrabook, perfect for course work");
        laptop.setCategory("Electronics");
        laptop.setPrice(new BigDecimal("3200.00"));
        laptop.setCoverImageUrl("https://dummyimage.com/600x360/1e90ff/ffffff.png&text=Laptop");
        laptop.setSellerId(1L);
        laptop.setPublishedAt(LocalDateTime.now().minusDays(1));
        laptop.setSold(false);
        laptop.setDeleted(false);
        laptop.setStatus(GoodsStatus.APPROVED.name());

        Goods textbook = new Goods();
        textbook.setTitle("Linear Algebra Textbook");
        textbook.setDescription("No notes, no damage");
        textbook.setCategory("Books");
        textbook.setPrice(new BigDecimal("25.00"));
        textbook.setCoverImageUrl("https://dummyimage.com/600x360/34d399/ffffff.png&text=Book");
        textbook.setSellerId(1L);
        textbook.setPublishedAt(LocalDateTime.now().minusHours(10));
        textbook.setSold(false);
        textbook.setDeleted(false);
        textbook.setStatus(GoodsStatus.APPROVED.name());

        Goods bike = new Goods();
        bike.setTitle("Campus Bicycle");
        bike.setDescription("Well maintained, lock included");
        bike.setCategory("Daily");
        bike.setPrice(new BigDecimal("280.00"));
        bike.setCoverImageUrl("https://dummyimage.com/600x360/f97316/ffffff.png&text=Bike");
        bike.setSellerId(1L);
        bike.setPublishedAt(LocalDateTime.now().minusHours(3));
        bike.setSold(false);
        bike.setDeleted(false);
        bike.setStatus(GoodsStatus.APPROVED.name());

        goodsMapper.insert(laptop);
        goodsMapper.insert(textbook);
        goodsMapper.insert(bike);
    }

    @Override
    public List<GoodsResponse> listGoods(GoodsFilterRequest request) {
        LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Goods::getSold, false);
        wrapper.eq(Goods::getDeleted, false);
        wrapper.eq(Goods::getStatus, GoodsStatus.APPROVED.name());
        if (request != null) {
            if (StringUtils.hasText(request.getCategory())) {
                wrapper.eq(Goods::getCategory, request.getCategory());
            }
            if (request.getMinPrice() != null) {
                wrapper.ge(Goods::getPrice, request.getMinPrice());
            }
            if (request.getMaxPrice() != null) {
                wrapper.le(Goods::getPrice, request.getMaxPrice());
            }
            if (StringUtils.hasText(request.getKeyword())) {
                wrapper.like(Goods::getTitle, request.getKeyword());
            }
        }
        wrapper.orderByDesc(Goods::getPublishedAt);
        return goodsMapper.selectList(wrapper)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public GoodsResponse getGoods(Long id, Long viewerId, boolean adminView) {
        Goods goods = getGoodsEntity(id);
        if (Boolean.TRUE.equals(goods.getDeleted())) {
            throw new IllegalArgumentException("Goods not found");
        }
        boolean isOwner = viewerId != null && viewerId.equals(goods.getSellerId());
        if (!adminView && !isOwner && !GoodsStatus.APPROVED.name().equals(goods.getStatus())) {
            throw new AccessDeniedException("无权限查看该商品");
        }
        return toResponse(goods);
    }

    @Override
    public Goods getGoodsEntity(Long id) {
        Goods goods = goodsMapper.selectById(id);
        if (goods == null) {
            throw new IllegalArgumentException("Goods not found");
        }
        if (!StringUtils.hasText(goods.getStatus())) {
            goods.setStatus(GoodsStatus.APPROVED.name());
        }
        return goods;
    }

    @Override
    public Goods createGoods(Goods goods) {
        goods.setPublishedAt(LocalDateTime.now());
        goods.setSold(false);
        goods.setDeleted(false);
        goods.setStatus(GoodsStatus.PENDING_REVIEW.name());
        if (!StringUtils.hasText(goods.getCoverImageUrl())) {
            goods.setCoverImageUrl(getFallbackImage(goods.getCategory()));
        }
        goodsMapper.insert(goods);
        return goods;
    }

    @Override
    public GoodsResponse updateGoods(Long id, GoodsUpdateRequest request, Long sellerId) {
        Goods goods = getGoodsEntity(id);
        if (goods == null || Boolean.TRUE.equals(goods.getDeleted())) {
            throw new IllegalArgumentException("Goods not found");
        }
        if (!goods.getSellerId().equals(sellerId)) {
            throw new AccessDeniedException("无权限操作该商品");
        }
        if (Boolean.TRUE.equals(goods.getSold())) {
            throw new IllegalArgumentException("商品已售出，无法修改");
        }
        goods.setDescription(request.getDescription());
        goods.setPrice(request.getPrice());
        if (StringUtils.hasText(request.getCoverImageUrl())) {
            goods.setCoverImageUrl(request.getCoverImageUrl());
        }
        goods.setStatus(GoodsStatus.PENDING_REVIEW.name());
        goodsMapper.updateById(goods);
        return toResponse(goods);
    }

    @Override
    public void deleteGoods(Long id, Long sellerId) {
        Goods goods = getGoodsEntity(id);
        if (goods == null || Boolean.TRUE.equals(goods.getDeleted())) {
            throw new IllegalArgumentException("Goods not found");
        }
        if (!goods.getSellerId().equals(sellerId)) {
            throw new AccessDeniedException("无权限操作该商品");
        }
        if (Boolean.TRUE.equals(goods.getSold())) {
            throw new IllegalArgumentException("商品已售出，无法删除");
        }
        goods.setDeleted(true);
        goodsMapper.updateById(goods);
    }

    @Override
    public List<GoodsResponse> listGoodsBySeller(Long sellerId) {
      return goodsMapper.selectList(new LambdaQueryWrapper<Goods>()
              .eq(Goods::getSellerId, sellerId)
              .eq(Goods::getDeleted, false)
              .orderByDesc(Goods::getPublishedAt))
          .stream()
          .map(this::toResponse)
          .collect(Collectors.toList());
    }

    @Override
    public List<GoodsResponse> listGoodsByStatus(GoodsStatus status) {
        return goodsMapper.selectList(new LambdaQueryWrapper<Goods>()
                .eq(Goods::getStatus, status.name())
                .eq(Goods::getDeleted, false)
                .orderByDesc(Goods::getPublishedAt))
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Override
    public GoodsResponse reviewGoods(Long id, GoodsStatus status) {
        Goods goods = getGoodsEntity(id);
        if (Boolean.TRUE.equals(goods.getDeleted())) {
            throw new IllegalArgumentException("Goods not found");
        }
        if (GoodsStatus.APPROVED.equals(status) && Boolean.TRUE.equals(goods.getSold())) {
            throw new IllegalArgumentException("商品已售出，无法审核通过");
        }
        goods.setStatus(status.name());
        if (GoodsStatus.APPROVED.equals(status)) {
            goods.setPublishedAt(LocalDateTime.now());
        }
        goodsMapper.updateById(goods);
        return toResponse(goods);
    }

    @Override
    public void markSold(Long goodsId, boolean sold) {
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null) {
            throw new IllegalArgumentException("Goods not found");
        }
        goods.setSold(sold);
        goodsMapper.updateById(goods);
    }

    private GoodsResponse toResponse(Goods goods) {
        String nickname = userService.findById(goods.getSellerId()).getNickname();
        return new GoodsResponse(
                goods.getId(),
                goods.getTitle(),
                goods.getDescription(),
                goods.getCategory(),
                goods.getPrice(),
                goods.getCoverImageUrl(),
                goods.getPublishedAt(),
                goods.getSellerId(),
                nickname,
                Boolean.TRUE.equals(goods.getSold()),
                goods.getStatus()
        );
    }

    private String getFallbackImage(String category) {
        if ("Electronics".equalsIgnoreCase(category)) {
            return "https://dummyimage.com/600x360/1e90ff/ffffff.png&text=Electronics";
        }
        if ("Books".equalsIgnoreCase(category)) {
            return "https://dummyimage.com/600x360/34d399/ffffff.png&text=Books";
        }
        if ("Daily".equalsIgnoreCase(category)) {
            return "https://dummyimage.com/600x360/f97316/ffffff.png&text=Daily";
        }
        return "https://dummyimage.com/600x360/f97316/ffffff.png&text=Goods";
    }
}
