package com.elasticsearch.cloud.monitor.metric.common.rule.condition.duration;

import com.elasticsearch.cloud.monitor.metric.common.rule.condition.JoinCondition;
import com.google.common.base.Joiner;

import java.util.List;

public abstract class JoinDurationCondition extends JoinCondition<DurationCondition> implements DurationCondition {

    public JoinDurationCondition(List<DurationCondition> conditions, Joiner joiner) {
        super(conditions, joiner);
    }

    @Override
    public long getCrossSpan() {
        long maxSpan = Long.MIN_VALUE;
        for (DurationCondition condition : getConditions()) {
            if (condition.getCrossSpan() > maxSpan) {
                maxSpan = condition.getCrossSpan();
            }
        }
        return maxSpan;
    }

    @Override
    public long getDurationTime() {
        long maxDuration = Long.MIN_VALUE;
        for (DurationCondition condition : getConditions()) {
            if (condition.getDurationTime() > maxDuration) {
                maxDuration = condition.getDurationTime();
            }
        }
        return maxDuration;
    }
}
