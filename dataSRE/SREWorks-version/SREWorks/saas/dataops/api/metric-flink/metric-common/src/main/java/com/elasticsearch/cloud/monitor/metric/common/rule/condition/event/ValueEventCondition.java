package com.elasticsearch.cloud.monitor.metric.common.rule.condition.event;

import com.elasticsearch.cloud.monitor.metric.common.checker.event.EventConditionChecker;
import com.elasticsearch.cloud.monitor.metric.common.checker.event.ValueEventConditionChecker;
import com.elasticsearch.cloud.monitor.metric.common.core.AlarmLevel;
import com.elasticsearch.cloud.monitor.metric.common.rule.condition.SingleCondition;
import com.elasticsearch.cloud.monitor.metric.common.rule.condition.ThresholdCondition;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Objects;

public class ValueEventCondition implements SingleCondition<EventCondition>, EventCondition {

    private final ThresholdCondition thresholdCondition;

    public ValueEventCondition(@JsonProperty("math_abs") boolean mathAbs, @JsonProperty("comparator") String comparator,
        @JsonProperty("thresholds") Map<AlarmLevel, Double> thresholds) {
        thresholdCondition = new ThresholdCondition(mathAbs, comparator, thresholds);
    }

    @Override
    public void validate() throws Exception {
        thresholdCondition.validate();
    }

    public AlarmLevel check(double value) {
        return thresholdCondition.getLevelOnValue(value);
    }

    @JsonProperty("math_abs")
    public boolean isMathAbs() {
        return thresholdCondition.isMathAbs();
    }

    @JsonProperty("comparator")
    public String getComparator() {
        return thresholdCondition.getComparator();
    }

    @JsonProperty("thresholds")
    public Map<AlarmLevel, Double> getThresholds() {
        return thresholdCondition.getThresholds();
    }

    @Override
    public EventConditionChecker getChecker() {
        return new ValueEventConditionChecker(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return Objects.equals(thresholdCondition, ((ValueEventCondition) o).thresholdCondition);
    }

    @Override
    public int hashCode() {
        return thresholdCondition != null ? thresholdCondition.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "ValueEventCondition{" + "mathAbs='" + isMathAbs() + '\'' + ", comparator='" + getComparator() + '\''
            + ", thresholds='" + getThresholds() + '\'' + '}';
    }
}
