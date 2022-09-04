package com.elasticsearch.cloud.monitor.metric.common.rule;

import com.elasticsearch.cloud.monitor.metric.common.rule.loader.RulesLoader;
import lombok.extern.slf4j.Slf4j;

/**
 * 规则管理类
 *
 * @author: fangzong.lyj
 * @date: 2021/09/01 10:45
 */
@Slf4j
public class OssRulesManager extends ClientRulesManager {

    public OssRulesManager(Object clientConfig, RulesLoader loader) {
        super(clientConfig, loader);
    }

    @Override
    protected RulesLoader initRuleLoader(Object clientConfig) {
        return null;
    }
}
