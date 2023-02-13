package com.elasticsearch.cloud.monitor.metric.common.rule.condition.duration;

import com.elasticsearch.cloud.monitor.metric.common.checker.duration.DurationConditionChecker;
import com.elasticsearch.cloud.monitor.metric.common.checker.duration.OrDurationConditionChecker;
import com.elasticsearch.cloud.monitor.metric.common.rule.condition.JoinCondition;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class OrDurationCondition extends JoinDurationCondition {

    @JsonCreator
    public OrDurationCondition(@JsonProperty("conditions") List<DurationCondition> conditions) {
        super(conditions, JoinCondition.OR_JOINER);
    }

    @Override
    public DurationConditionChecker getChecker() {
        return new OrDurationConditionChecker(this);
    }
}
