package com.elasticsearch.cloud.monitor.metric.common.rule.condition.duration;

import com.elasticsearch.cloud.monitor.metric.common.core.AlarmLevel;
import com.elasticsearch.cloud.monitor.metric.common.core.Constants;
import com.elasticsearch.cloud.monitor.metric.common.utils.TimeUtils;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Objects;

public abstract class CompareDurationCondition extends ThresholdDurationCondition {
    private final String comparedTo;

    public CompareDurationCondition(final String duration, final String comparedTo, final boolean mathAbs,
        final String comparator, final Map<AlarmLevel, Double> thresholds) {
        super(duration, mathAbs, comparator, thresholds);
        this.comparedTo = comparedTo;
    }

    @Override
    public void validate() throws Exception {
        super.validate();
        Preconditions.checkArgument(StringUtils.isNotEmpty(comparedTo), "comparedTo must not be empty");
        long comparedTo = TimeUtils.parseDuration(this.comparedTo);
        long duration = TimeUtils.parseDuration(getDuration());
        Preconditions.checkArgument(comparedTo >= duration,
            "comparedTo(" + comparedTo + ") must be >= duration(" + duration + ")");
        Preconditions.checkArgument(comparedTo % Constants.CHECK_INTERVAL == 0,
            "comparedTo(" + comparedTo + ") must be a multiple of " + Constants.CHECK_INTERVAL);
        Preconditions.checkArgument(duration <= Constants.MAX_DURATION_TIME,
            "duration value(" + duration + ") must be less than " + Constants.MAX_DURATION_TIME);
        Preconditions.checkArgument(comparedTo <= Constants.MAX_COMPARE_TO_TIME,
            "comparedTo value(" + comparedTo + ") must be less than " + Constants.MAX_COMPARE_TO_TIME);
    }

    @JsonProperty("compared_to")
    public String getComparedTo() {
        return comparedTo;
    }


    @Override
    public long getCrossSpan() {
        return super.getCrossSpan() + TimeUtils.parseDuration(comparedTo);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CompareDurationCondition that = (CompareDurationCondition) o;

        return Objects.equals(comparedTo, that.comparedTo);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (comparedTo != null ? comparedTo.hashCode() : 0);
        return result;
    }

}
