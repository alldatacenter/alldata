package com.elasticsearch.cloud.monitor.metric.common.rule.condition.duration;

import com.elasticsearch.cloud.monitor.metric.common.checker.duration.DurationConditionChecker;
import com.elasticsearch.cloud.monitor.metric.common.rule.condition.Condition;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {@JsonSubTypes.Type(name = "and", value = AndDurationCondition.class),
    @JsonSubTypes.Type(name = "or", value = OrDurationCondition.class),
    @JsonSubTypes.Type(name = "continuous", value = ContinuousDurationCondition.class),
    @JsonSubTypes.Type(name = "value_threshold", value = ValueDurationCondition.class),
    @JsonSubTypes.Type(name = "change_threshold", value = ChangeDurationCondition.class),
    @JsonSubTypes.Type(name = "rate_change_threshold", value = RateChangeDurationCondition.class),})
public interface DurationCondition extends Condition {

    @JsonIgnore
    DurationConditionChecker getChecker();

    @JsonIgnore
    long getCrossSpan();

    @JsonIgnore
    long getDurationTime();
}
