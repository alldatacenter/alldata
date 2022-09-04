package com.elasticsearch.cloud.monitor.metric.common.pojo;

import lombok.Getter;

/**
 * @author xiaoping
 * @date 2020/6/5
 */
@Getter
public enum Granularity {
    PRECISE("1s"),
    CRITICAL("5s"),
    MAJOR("10s"),
    NORMAL("20s"),
    TRIVIAL("1m");
    private String value;


    Granularity(String value) {
        this.value = value;
    }
}
