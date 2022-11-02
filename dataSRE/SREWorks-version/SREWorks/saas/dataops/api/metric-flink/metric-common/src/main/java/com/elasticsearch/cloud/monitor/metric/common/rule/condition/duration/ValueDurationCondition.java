package com.elasticsearch.cloud.monitor.metric.common.rule.condition.duration;

import com.elasticsearch.cloud.monitor.metric.common.checker.duration.DurationConditionChecker;
import com.elasticsearch.cloud.monitor.metric.common.checker.duration.ValueDurationConditionChecker;
import com.elasticsearch.cloud.monitor.metric.common.core.AlarmLevel;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;


public class ValueDurationCondition extends ThresholdDurationCondition {

    @JsonCreator
    public ValueDurationCondition(@JsonProperty("duration") String duration, @JsonProperty("math_abs") boolean mathAbs,
        @JsonProperty("comparator") String comparator, @JsonProperty("thresholds") Map<AlarmLevel, Double> thresholds) {
        super(duration, mathAbs, comparator, thresholds);
    }

    @Override
    public DurationConditionChecker getChecker() {
        return new ValueDurationConditionChecker(this);
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return "ValueDurationCondition{" + "duration='" + getDuration() + '\'' + ", mathAbs='" + isMathAbs() + '\''
            + ", comparator='" + getComparator() + '\'' + ", thresholds='" + getThresholds() + '\'' + '}';
    }

}
