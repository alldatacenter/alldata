package com.elasticsearch.cloud.monitor.metric.common.rule.condition.event;

import com.elasticsearch.cloud.monitor.metric.common.checker.event.AndEventConditionChecker;
import com.elasticsearch.cloud.monitor.metric.common.rule.condition.JoinCondition;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class AndEventCondition extends JoinCondition<EventCondition> implements EventCondition {

    public AndEventCondition(@JsonProperty("event_conditions") List<EventCondition> conditions) {
        super(conditions, JoinCondition.AND_JOINER);
    }

    @Override
    public AndEventConditionChecker getChecker() {
        return new AndEventConditionChecker(this);
    }
}
