package com.platform.task.entity;

public class RecommendEntity {
    private String productId;
    private Double sim;

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Double getSim() {
        return sim;
    }

    public void setSim(Double sim) {
        this.sim = sim;
    }

    public RecommendEntity() {
    }

    public RecommendEntity(String productId, Double sim) {
        this.productId = productId;
        this.sim = sim;
    }

    @Override
    public String toString() {
        return "RecommendEntity{" +
                "productId='" + productId + '\'' +
                ", sim=" + sim +
                '}';
    }
}
