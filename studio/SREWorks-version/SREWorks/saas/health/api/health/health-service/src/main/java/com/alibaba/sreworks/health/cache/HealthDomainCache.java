package com.alibaba.sreworks.health.cache;

import com.alibaba.sreworks.health.common.cache.HealthCache;

/**
 * 健康管理domaincache
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/03 16:30
 */
public abstract class HealthDomainCache<T> implements HealthCache<T> {
    /**
     * 最大缓存数量
     */
     protected int maxCacheSize;
}
