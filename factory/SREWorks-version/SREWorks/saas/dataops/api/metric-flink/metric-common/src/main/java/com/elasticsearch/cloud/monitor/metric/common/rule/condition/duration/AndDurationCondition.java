package com.elasticsearch.cloud.monitor.metric.common.rule.condition.duration;

import com.elasticsearch.cloud.monitor.metric.common.checker.duration.AndDurationConditionChecker;
import com.elasticsearch.cloud.monitor.metric.common.checker.duration.DurationConditionChecker;
import com.elasticsearch.cloud.monitor.metric.common.rule.condition.JoinCondition;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class AndDurationCondition extends JoinDurationCondition {

    @JsonCreator
    public AndDurationCondition(@JsonProperty("conditions") List<DurationCondition> conditions) {
        super(conditions, JoinCondition.AND_JOINER);
    }

    @Override
    public DurationConditionChecker getChecker() {
        return new AndDurationConditionChecker(this);
    }
}
