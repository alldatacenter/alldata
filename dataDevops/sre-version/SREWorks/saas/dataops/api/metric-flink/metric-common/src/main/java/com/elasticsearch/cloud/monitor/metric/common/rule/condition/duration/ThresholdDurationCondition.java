package com.elasticsearch.cloud.monitor.metric.common.rule.condition.duration;

import com.elasticsearch.cloud.monitor.metric.common.core.AlarmLevel;
import com.elasticsearch.cloud.monitor.metric.common.core.Constants;
import com.elasticsearch.cloud.monitor.metric.common.rule.condition.SingleCondition;
import com.elasticsearch.cloud.monitor.metric.common.rule.condition.ThresholdCondition;
import com.elasticsearch.cloud.monitor.metric.common.utils.TimeUtils;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Objects;

public abstract class ThresholdDurationCondition implements SingleCondition<DurationCondition>, DurationCondition {
    private final String duration;
    private final ThresholdCondition thresholdCondition;

    public ThresholdDurationCondition(final String duration, final boolean mathAbs, final String comparator,
        final Map<AlarmLevel, Double> thresholds) {
        this.duration = duration;
        thresholdCondition = new ThresholdCondition(mathAbs, comparator, thresholds);
    }

    @Override
    public void validate() throws Exception {
        thresholdCondition.validate();
        Preconditions.checkArgument(StringUtils.isNotEmpty(duration), "duration must not be empty");

        long durationTime = TimeUtils.parseDuration(duration);
        Preconditions.checkArgument(durationTime >= Constants.CHECK_INTERVAL,
            "duration(" + duration + ") must be >= " + Constants.CHECK_INTERVAL_STR);
        Preconditions.checkArgument(durationTime % Constants.CHECK_INTERVAL == 0,
            "duration(" + duration + ") must be a multiple of " + Constants.CHECK_INTERVAL_STR);
        Preconditions.checkArgument(durationTime <= Constants.MAX_DURATION_TIME,
            "duration value(" + duration + ") must be less than " + Constants.MAX_DURATION_TIME);
    }

    public AlarmLevel check(double value) {
        return thresholdCondition.getLevelOnValue(value);
    }

    @Override
    public long getCrossSpan() {
        return getDurationTime();
    }

    @Override
    public long getDurationTime() {
        return TimeUtils.parseDuration(duration);
    }

    @JsonProperty("duration")
    public String getDuration() {
        return duration;
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ThresholdDurationCondition that = (ThresholdDurationCondition) o;

        if (!Objects.equals(duration, that.duration)) {
            return false;
        }
        return Objects.equals(thresholdCondition, that.thresholdCondition);
    }

    @Override
    public int hashCode() {
        int result = duration != null ? duration.hashCode() : 0;
        result = 31 * result + (thresholdCondition != null ? thresholdCondition.hashCode() : 0);
        return result;
    }
}
