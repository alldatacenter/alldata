package com.elasticsearch.cloud.monitor.metric.common.rule.condition.duration;

import com.elasticsearch.cloud.monitor.metric.common.checker.duration.ContinuousDurationConditionChecker;
import com.elasticsearch.cloud.monitor.metric.common.checker.duration.DurationConditionChecker;
import com.elasticsearch.cloud.monitor.metric.common.core.Constants;
import com.elasticsearch.cloud.monitor.metric.common.rule.condition.CompositeCondition;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;

import java.util.Objects;

public class ContinuousDurationCondition implements CompositeCondition<DurationCondition>, DurationCondition {
    private final int count;
    private final DurationCondition condition;

    @JsonCreator
    public ContinuousDurationCondition(@JsonProperty("count") int count,
        @JsonProperty("condition") DurationCondition condition) {
        this.count = count;
        this.condition = condition;
    }

    @Override
    public void validate() throws Exception {
        Preconditions.checkArgument(count > 0, "count must be greater than 0");
        Preconditions.checkArgument(condition != null, "condition must not be null");
        condition.validate();
        long duration = getDurationTime();
        Preconditions.checkArgument(duration <= Constants.MAX_DURATION_TIME,
            "duration value * count (" + duration + ") must be less than " + Constants.MAX_DURATION_TIME);
    }

    @Override
    public DurationConditionChecker getChecker() {
        return new ContinuousDurationConditionChecker(this);
    }

    @Override
    public long getCrossSpan() {
        return condition.getCrossSpan() + (count - 1) * Constants.CHECK_INTERVAL;
    }

    @Override
    public long getDurationTime() {
        return condition.getDurationTime() + (count - 1) * Constants.CHECK_INTERVAL;
    }

    @JsonProperty("count")
    public int getCount() {
        return count;
    }

    @JsonProperty("condition")
    public DurationCondition getDurationCondition() {
        return condition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ContinuousDurationCondition that = (ContinuousDurationCondition) o;

        if (count != that.count) {
            return false;
        }
        return Objects.equals(condition, that.condition);
    }

    @Override
    public int hashCode() {
        int result = count;
        result = 31 * result + (condition != null ? condition.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ContinuousDurationCondition{" + "count=" + count + ", condition=" + condition + '}';
    }
}
