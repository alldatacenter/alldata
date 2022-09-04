package com.elasticsearch.cloud.monitor.metric.common.checker.duration;

import com.elasticsearch.cloud.monitor.metric.common.core.Alarm;
import com.elasticsearch.cloud.monitor.metric.common.core.AlarmLevel;
import com.elasticsearch.cloud.monitor.metric.common.datapoint.DataPoint;
import com.elasticsearch.cloud.monitor.metric.common.provider.DataProvider;
import com.elasticsearch.cloud.monitor.metric.common.rule.condition.duration.ContinuousDurationCondition;

import java.util.LinkedList;

public class ContinuousDurationConditionChecker extends CompositeDurationConditionChecker {
    private final int thresholdCount;
    private final DurationConditionChecker checker;
    private final LinkedList<Alarm> queue;
    private final int maxQueueSize;
    private int continuousCount = 0;

    public ContinuousDurationConditionChecker(final ContinuousDurationCondition condition) {
        checker = condition.getDurationCondition().getChecker();
        thresholdCount = condition.getCount();
        maxQueueSize = Math.min(thresholdCount, 5);
        queue = new LinkedList<>();
    }

    @Override
    public Alarm check(final DataPoint dp, final DataProvider dataProvider) {
        Alarm alarm = checker.check(dp, dataProvider);
        if (alarm != null && alarm.getDataMissed()) {
            return null;
        }

        if (alarm == null || alarm.isOk()) {
            queue.clear();
            continuousCount = 0;
            return alarm;
        }
        continuousCount++;
        queue.addFirst(alarm);
        if (queue.size() > maxQueueSize) {
            queue.removeLast();
        }
        if (continuousCount < thresholdCount) {
            return new Alarm(
                AlarmLevel.NORMAL, "连续" + continuousCount + "次[" + alarm.getMsg() + "], 未超过" + thresholdCount + "次");
        }

        // find the most newest alarm
        Alarm newest = queue.getFirst();
        return new Alarm(newest.getLevel(), "连续" + continuousCount + "次[" + newest.getMsg() + "]");
    }
}
