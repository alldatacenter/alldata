//package com.elasticsearch.cloud.monitor.metric.common.rule;
//
//import com.google.common.collect.Maps;
//import lombok.extern.slf4j.Slf4j;
//
//import java.util.Map;
//
///**
// * minio规则管理生成类
// *
// * @author: fangzong.lyj
// * @date: 2021/08/31 21:15
// */
//@Slf4j
//public abstract class RuleManagerFactory {
//    protected Map<String, ClientRulesManager> rulesManagers;
//    protected long RULE_REFRESH_PERIOD_DEF = 90000L;
//    protected long RULE_REFRESH_SHUFFLE_DEF = 60000L;
//    private volatile boolean first;
//
//
//    public RuleManagerFactory(Object clientConfig, Long refreshPeriod, Long shufflePeriod) {
//        rulesManagers = Maps.newHashMap();
//        first = true;
//        if (refreshPeriod != null) {
//            RULE_REFRESH_PERIOD_DEF = refreshPeriod;
//        }
//
//        if (shufflePeriod != null) {
//            RULE_REFRESH_SHUFFLE_DEF = shufflePeriod;
//        }
//
//        this.init(clientConfig);
//    }
//
//    public RuleManagerFactory(Object clientConfig) {
//        this(clientConfig, null, null);
//    }
//
//    protected abstract void init(Object clientConfig);
//
//    /**
//     * 租户, 按需返回规则配置文件
//     *
//     * @param tenant
//     * @return
//     */
//    public ClientRulesManager getRuleManager(String tenant) {
//        ClientRulesManager rulesManager = null;
//        if (rulesManagers != null && !rulesManagers.isEmpty()) {
//            rulesManager = rulesManagers.getOrDefault(tenant, null);
//        }
//
//        if (rulesManager == null) {
//            log.warn(String.format("rulManager for tenant %s not exist", tenant));
//        }
//        return rulesManager;
//    }
//
//    /**
//     * 无租户, 简单实现 默认返回第一个规则配置文件
//     *
//     * @return
//     */
//    public ClientRulesManager getRuleManager() {
//        ClientRulesManager rulesManager = null;
//        if (rulesManagers != null && !rulesManagers.isEmpty()) {
//            rulesManager = rulesManagers.values().iterator().next();
//        }
//
//        if (rulesManager == null) {
//            log.warn("rulManager is empty");
//        }
//        return rulesManager;
//    }
//
//    public void close() {
//        if (this.rulesManagers != null) {
//            rulesManagers.values().forEach(ClientRulesManager::stopUpdateTiming);
//        }
//    }
//
//    public Map<String, ClientRulesManager> getRulesManagers() {
//        return rulesManagers;
//    }
//
//    public long getRULE_REFRESH_PERIOD_DEF() {
//        return RULE_REFRESH_PERIOD_DEF;
//    }
//
//    public long getRULE_REFRESH_SHUFFLE_DEF() {
//        return RULE_REFRESH_SHUFFLE_DEF;
//    }
//
//    public boolean isFirst() {
//        return first;
//    }
//}
