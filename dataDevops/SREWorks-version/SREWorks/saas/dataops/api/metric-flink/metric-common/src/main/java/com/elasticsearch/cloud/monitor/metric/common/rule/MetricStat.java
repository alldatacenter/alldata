package com.elasticsearch.cloud.monitor.metric.common.rule;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by sealeaf on 2018/10/12
 **/
public class MetricStat {
    @Getter@Setter
    private int totalRulesCnt;
    @Getter@Setter
    private int totalSplitRulesCnt;
    @Getter@Setter
    private int totalWildcardRulesCnt;
    @Getter@Setter
    private int totalMultiMetricRulesCnt;
    @Getter@Setter
    private int totalSingleMetricRulesCnt;
    @Getter@Setter
    private int totalApackRulesCnt;
    @Getter@Setter
    private AtomicLong cacheHitCnt = new AtomicLong();
    @Getter@Setter
    private AtomicLong cacheMissCnt = new AtomicLong();
    @Getter@Setter
    private AtomicLong crossJoinCnt = new AtomicLong();
    @Getter@Setter
    private AtomicLong emptyMetricHitCnt = new AtomicLong();
    @Getter@Setter
    private AtomicLong crossJoinMatchCnt = new AtomicLong();
    @Getter@Setter
    private AtomicLong downStreamCnt = new AtomicLong();
}
