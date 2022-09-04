package com.elasticsearch.cloud.monitor.metric.common.rule.condition.event;

import com.elasticsearch.cloud.monitor.metric.common.checker.event.OrEventConditionChecker;
import com.elasticsearch.cloud.monitor.metric.common.rule.condition.JoinCondition;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class OrEventCondition extends JoinCondition<EventCondition> implements EventCondition {

    public OrEventCondition(@JsonProperty("event_conditions") List<EventCondition> conditions) {
        super(conditions, JoinCondition.OR_JOINER);
    }

    @Override
    public OrEventConditionChecker getChecker() {
        return new OrEventConditionChecker(this);
    }
}
