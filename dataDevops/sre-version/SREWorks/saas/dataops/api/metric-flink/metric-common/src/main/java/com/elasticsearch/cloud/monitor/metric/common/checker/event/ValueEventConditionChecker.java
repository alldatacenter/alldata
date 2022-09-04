package com.elasticsearch.cloud.monitor.metric.common.checker.event;

import com.elasticsearch.cloud.monitor.metric.common.core.Alarm;
import com.elasticsearch.cloud.monitor.metric.common.core.AlarmLevel;
import com.elasticsearch.cloud.monitor.metric.common.datapoint.DataPoint;
import com.elasticsearch.cloud.monitor.metric.common.rule.condition.event.ValueEventCondition;

public class ValueEventConditionChecker implements EventConditionChecker {

    private final ValueEventCondition condition;

    public ValueEventConditionChecker(final ValueEventCondition condition) {
        this.condition = condition;
    }

    @Override
    public Alarm check(final DataPoint dp) {
        double value = dp.getValue();
        AlarmLevel level = condition.check(value);
        if (level != null) {
            return new Alarm(level, getMsg(value, level));
        } else {
            return null;
        }
    }

    private String getMsg(final double value, final AlarmLevel level) {

        StringBuilder sb = new StringBuilder();
        sb.append("当前值");
        sb.append(String.format("%.2f", condition.isMathAbs() ? Math.abs(value) : value));

        if(AlarmLevel.NORMAL.equals(level)){
            return sb.toString();
        }

        sb.append(condition.getComparator());
        sb.append("阈值");

        double threshold = condition.getThresholds().get(level);
        sb.append(String.format("%.2f", threshold));
        return sb.toString();
    }
}
