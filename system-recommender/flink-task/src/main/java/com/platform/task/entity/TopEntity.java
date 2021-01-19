package com.platform.task.entity;

public class TopEntity {
    private int productId;
    private long actionTimes;
    private long windowEnd;
    private String rankName;

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public long getActionTimes() {
        return actionTimes;
    }

    public void setActionTimes(long actionTimes) {
        this.actionTimes = actionTimes;
    }

    public long getWindowEnd() {
        return windowEnd;
    }

    public void setWindowEnd(long windowEnd) {
        this.windowEnd = windowEnd;
    }

    public String getRankName() {
        return rankName;
    }

    public void setRankName(String rankName) {
        this.rankName = rankName;
    }

    public static TopEntity getTopEntity(Integer id, Long end, Long count) {
        TopEntity topEntity = new TopEntity();
        topEntity.setActionTimes(count);
        topEntity.setProductId(id);
        topEntity.setRankName(String.valueOf(end));
        topEntity.setWindowEnd(end);
        return topEntity;
    }

    @Override
    public String toString() {
        return "TopEntity{" +
                "productId=" + productId +
                ", actionTimes=" + actionTimes +
                ", windowEnd=" + windowEnd +
                ", rankName='" + rankName + '\'' +
                '}';
    }
}
