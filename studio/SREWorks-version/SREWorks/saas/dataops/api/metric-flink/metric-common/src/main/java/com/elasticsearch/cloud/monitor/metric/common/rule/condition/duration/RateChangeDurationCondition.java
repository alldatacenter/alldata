package com.elasticsearch.cloud.monitor.metric.common.rule.condition.duration;

import com.elasticsearch.cloud.monitor.metric.common.checker.duration.DurationConditionChecker;
import com.elasticsearch.cloud.monitor.metric.common.checker.duration.RateChangeDurationConditionChecker;
import com.elasticsearch.cloud.monitor.metric.common.core.AlarmLevel;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class RateChangeDurationCondition extends CompareDurationCondition {

    @JsonCreator
    public RateChangeDurationCondition(@JsonProperty("duration") String duration,
        @JsonProperty("compared_to") String comparedTo, @JsonProperty("math_abs") boolean mathAbs,
        @JsonProperty("comparator") String comparator, @JsonProperty("thresholds") Map<AlarmLevel, Double> thresholds) {
        super(duration, comparedTo, mathAbs, comparator, thresholds);
    }

    @Override
    public DurationConditionChecker getChecker() {
        return new RateChangeDurationConditionChecker(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "RateChangeDurationCondition{" + "duration='" + getDuration() + '\'' + ", comparedTo='" + getComparator()
            + '\'' + ", mathAbs='" + isMathAbs() + '\'' + ", comparator='" + getComparator() + '\'' + ", thresholds='"
            + getThresholds() + '\'' + '}';
    }

}
