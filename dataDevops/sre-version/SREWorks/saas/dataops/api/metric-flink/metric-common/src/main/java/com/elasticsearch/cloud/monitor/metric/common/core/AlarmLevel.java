package com.elasticsearch.cloud.monitor.metric.common.core;

public enum AlarmLevel {
    CRITICAL(300), ERROR(200), WARNING(100),  NORMAL(0);

    private int value;

    AlarmLevel(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
