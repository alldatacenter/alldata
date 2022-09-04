package com.elasticsearch.cloud.monitor.metric.common.rule;

import com.elasticsearch.cloud.monitor.metric.common.rule.loader.HealthAlertRulesLoader;
import com.elasticsearch.cloud.monitor.metric.common.rule.loader.RulesLoader;
import lombok.extern.slf4j.Slf4j;

/**
 * 规则管理类
 *
 * @author: fangzong.lyj
 * @date: 2021/09/01 10:45
 */
@Slf4j
public class HealthAlertRulesManager extends ClientRulesManager {

    public HealthAlertRulesManager(String rulesConfig) {
        this(rulesConfig,null);
    }

    public HealthAlertRulesManager(String rulesConfig, RulesLoader loader) {
        super(rulesConfig, loader);
    }

    @Override
    protected RulesLoader initRuleLoader(Object rulesConfig) {
        log.info("begin init health alert rule loader");
        return initHealthAlertRuleLoader((String) rulesConfig);
    }

    private RulesLoader initHealthAlertRuleLoader(String rulesConfig) {
        RulesLoader rulesLoader = new HealthAlertRulesLoader(rulesConfig);;
        return rulesLoader;
    }

}
