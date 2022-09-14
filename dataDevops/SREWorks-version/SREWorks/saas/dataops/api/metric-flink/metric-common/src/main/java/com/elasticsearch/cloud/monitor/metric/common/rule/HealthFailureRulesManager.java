package com.elasticsearch.cloud.monitor.metric.common.rule;

import com.alibaba.fastjson.JSONObject;
import com.elasticsearch.cloud.monitor.metric.common.rule.failure.FailureLevelWithBoundary;
import com.elasticsearch.cloud.monitor.metric.common.rule.failure.FailureRule;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 规则管理类
 *
 * @author: fangzong.lyj
 * @date: 2021/09/01 10:45
 */
@Slf4j
public class HealthFailureRulesManager extends SreworksRulesManager {

    private FailureRule failureRule;

    private Timer updateTimer;

    public HealthFailureRulesManager(String rulesConfig) {
        failureRule = new FailureRule();
        initRule(JSONObject.parseObject(rulesConfig));
    }

    protected void initRule(JSONObject rulesConfig) {
        log.info("begin init failure rule loader");
        failureRule.checkAndLoad(rulesConfig);
    }

    public FailureLevelWithBoundary matchRule(Long duration) {
        return failureRule.getFailureLevel(duration);
    }

    private void checkAndLoad() {

    }

    public void startShuffleTimingUpdate(long period, long shuffle) {
        Random random = new Random();
        long delta = (long)(random.nextInt((int)(shuffle / 1000L)) * 1000);
        startTimingUpdate(period + delta);
    }

    public void startTimingUpdate(long period) {
        Preconditions.checkArgument(period > 0L, "period <= 0");
        this.updateTimer = new Timer(true);
        this.updateTimer.schedule(new TimerTask() {
            public void run() {
                checkAndLoad();
            }
        }, period, period);
    }

    public void stopUpdateTiming() {
        if (updateTimer != null) {
            updateTimer.cancel();
            updateTimer.purge();
        }
    }

}
