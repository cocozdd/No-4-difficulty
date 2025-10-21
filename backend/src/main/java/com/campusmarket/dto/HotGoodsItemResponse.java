package com.campusmarket.dto;

public class HotGoodsItemResponse {

    private GoodsResponse goods;
    private double score;

    public HotGoodsItemResponse() {
    }

    public HotGoodsItemResponse(GoodsResponse goods, double score) {
        this.goods = goods;
        this.score = score;
    }

    public GoodsResponse getGoods() {
        return goods;
    }

    public void setGoods(GoodsResponse goods) {
        this.goods = goods;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}
