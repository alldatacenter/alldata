package com.alibaba.sreworks.dataset.processors;

public enum ESMetricAggType {
    MAX("MAX"),
    MIN("MIN"),
    AVG("AVG"),
    SUM("SUM"),
    CARDINALITY("CARDINALITY"),
    DERIVATIVE("DERIVATIVE");

    private String type;

    ESMetricAggType(String type) {
        this.type = type;
    }

    public String getType() {
        return this.type;
    }
}
