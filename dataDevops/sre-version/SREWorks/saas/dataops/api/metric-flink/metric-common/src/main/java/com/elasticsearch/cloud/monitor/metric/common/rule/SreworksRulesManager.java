package com.elasticsearch.cloud.monitor.metric.common.rule;

import lombok.extern.slf4j.Slf4j;


/**
 * 规则管理类
 *
 * @author: fangzong.lyj
 * @date: 2021/09/01 10:45
 */
@Slf4j
public abstract class SreworksRulesManager {
    public abstract void startShuffleTimingUpdate(long period, long shuffle);

    public abstract void startTimingUpdate(long period);

    public abstract void stopUpdateTiming();
}
