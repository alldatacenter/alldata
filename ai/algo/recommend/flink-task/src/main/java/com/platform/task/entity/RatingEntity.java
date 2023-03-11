package com.platform.schedule.entity;

public class RatingEntity {
    private Integer userId;
    private Integer productId;
    private Double score;
    private Integer timestamp;

    public RatingEntity() {
    }

    public RatingEntity(Integer userId, Integer productId, Double score, Integer timestamp) {
        this.userId = userId;
        this.productId = productId;
        this.score = score;
        this.timestamp = timestamp;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Integer getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Integer timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "RatingEntity{" +
                "userId=" + userId +
                ", productId=" + productId +
                ", score=" + score +
                ", timestamp=" + timestamp +
                '}';
    }
}
