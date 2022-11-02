package com.elasticsearch.cloud.monitor.metric.common.rule;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * SW规则管理生成类
 *
 * @author: fangzong.lyj
 * @date: 2022/01/19 14:50
 */
@Slf4j
public abstract class SreworksRulesManagerFactory<T extends SreworksRulesManager> {
    protected Cache<String, T> rulesManagers;
    protected long RULE_REFRESH_PERIOD_DEF = 90000L;
    protected long RULE_REFRESH_SHUFFLE_DEF = 60000L;
    private volatile boolean first;

    public SreworksRulesManagerFactory(Object config, Long refreshPeriod, Long shufflePeriod) {
        first = true;

        if (refreshPeriod != null) {
            RULE_REFRESH_PERIOD_DEF = refreshPeriod;
        }

        if (shufflePeriod != null) {
            RULE_REFRESH_SHUFFLE_DEF = shufflePeriod;
        }
        rulesManagers = CacheBuilder.newBuilder().expireAfterWrite(RULE_REFRESH_PERIOD_DEF, TimeUnit.MILLISECONDS).build();

        if (Objects.nonNull(config)) {
            load(config);
        }
    }

    public SreworksRulesManagerFactory(Long refreshPeriod, Long shufflePeriod) {
        this(null, refreshPeriod, shufflePeriod);
    }

    public SreworksRulesManagerFactory() {
        this(null, null);
    }

    /**
     * 规则管理实例化
     * @param config
     */
    protected abstract T load(Object config);

    /**
     * 根据KEY返回规则管理对象
     *
     * @return
     */
    public void initRulesManager(String key, Object config) {
        T rulesManager = load(config);
        rulesManagers.put(key, rulesManager);
    }

    /**
     * 根据KEY返回规则管理对象
     *
     * @return
     */
    public T getRulesManager(String key) {
        T rulesManager = rulesManagers.getIfPresent(key);
        if (rulesManager == null) {
            log.warn(String.format("not exist rules manager[key:%s]", key));
        }
        return rulesManager;
    }

    /**
     * 根据KEY返回规则管理对象
     *
     * @return
     */
    public T getRulesManager(String key, Object config) {
        T rulesManager = rulesManagers.getIfPresent(key);
        if (rulesManager == null) {
            log.warn(String.format("not exist rules manager[key:%s]", key));
            rulesManager = load(config);
            rulesManagers.put(key, rulesManager);
        }
        return rulesManager;
    }

    /**
     * 无租户, 简单实现 默认返回第一个规则管理对象
     *
     * @return
     */
    public T getRulesManager() {
        T rulesManager = null;
        if (rulesManagers != null && rulesManagers.size() > 0) {
            rulesManager = rulesManagers.asMap().values().iterator().next();
        }

        if (rulesManager == null) {
            log.warn("rulManager is empty");
        }
        return rulesManager;
    }

    public void close() {
        rulesManagers.asMap().forEach((rulesKey, rulesManager) -> {
            rulesManager.stopUpdateTiming();
        });
        rulesManagers.cleanUp();
    }

    public Cache<String, T> getRulesManagers() {
        return rulesManagers;
    }

    public long getRULE_REFRESH_PERIOD_DEF() {
        return RULE_REFRESH_PERIOD_DEF;
    }

    public long getRULE_REFRESH_SHUFFLE_DEF() {
        return RULE_REFRESH_SHUFFLE_DEF;
    }

    public boolean isFirst() {
        return first;
    }
}
