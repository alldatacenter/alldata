package com.elasticsearch.cloud.monitor.metric.common.rule;

import com.elasticsearch.cloud.monitor.metric.common.client.MinioConfig;
import lombok.extern.slf4j.Slf4j;


/**
 * minio规则管理生成类
 *
 * @author: fangzong.lyj
 * @date: 2021/08/31 21:15
 */
@Slf4j
public class MinioRulesManagerFactory extends SreworksRulesManagerFactory<MinioRulesManager> {
    private MinioConfig minioConfig;

    public MinioRulesManagerFactory(Object config, Long refreshPeriod, Long shufflePeriod) {
        super(config, refreshPeriod, shufflePeriod);
    }

    @Override
    protected MinioRulesManager load(Object clientConfig) {
        minioConfig = (MinioConfig)clientConfig;
        MinioRulesManager rulesManager = new MinioRulesManager(minioConfig);
        rulesManager.startShuffleTimingUpdate(this.RULE_REFRESH_PERIOD_DEF, this.RULE_REFRESH_SHUFFLE_DEF);
        return rulesManager;
//        this.rulesManagers.put(minioConfig.getBucket() + '/' + minioConfig.getFile(), rulesManager);
    }
}
