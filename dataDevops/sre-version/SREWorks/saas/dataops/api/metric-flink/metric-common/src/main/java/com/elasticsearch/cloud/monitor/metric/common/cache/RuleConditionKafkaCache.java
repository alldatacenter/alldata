package com.elasticsearch.cloud.monitor.metric.common.cache;

import com.elasticsearch.cloud.monitor.metric.common.client.KafkaConfig;
import com.elasticsearch.cloud.monitor.metric.common.datapoint.DataPoint;
import com.elasticsearch.cloud.monitor.metric.common.rule.Rule;

import java.io.IOException;

/**
 * 规则缓存(Kafka源)
 *
 * @author: fangzong.lyj
 * @date: 2021/08/31 19:58
 */
public class RuleConditionKafkaCache extends RuleConditionCache{

    public RuleConditionKafkaCache(Rule rule, long interval, KafkaConfig kafkaConfig) {
        super(rule, interval);
    }

    @SuppressWarnings("Duplicates")
    @Override
    public void recovery(DataPoint dataPoint) throws IOException {

    }
}
