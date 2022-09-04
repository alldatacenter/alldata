package com.elasticsearch.cloud.monitor.metric.common.rule;

import lombok.extern.slf4j.Slf4j;


/**
 * Oss规则管理生成类
 *
 * @author: fangzong.lyj
 * @date: 2021/08/31 21:15
 */
@Slf4j
public class OssRulesManagerFactory extends SreworksRulesManagerFactory<OssRulesManager> {

    public OssRulesManagerFactory(Object config, Long refreshPeriod, Long shufflePeriod) {
        super(config, refreshPeriod, shufflePeriod);
    }

    @Override
    protected OssRulesManager load(Object clientConfig) {
        return null;
    }
}
