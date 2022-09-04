package com.alibaba.sreworks.health.common.cache;

public interface HealthCache<T> {
    void reconstructCache(T reconstructor);
}
