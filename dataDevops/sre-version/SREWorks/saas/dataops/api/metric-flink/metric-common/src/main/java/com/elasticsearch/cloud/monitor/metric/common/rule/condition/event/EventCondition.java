package com.elasticsearch.cloud.monitor.metric.common.rule.condition.event;

import com.elasticsearch.cloud.monitor.metric.common.checker.event.EventConditionChecker;
import com.elasticsearch.cloud.monitor.metric.common.rule.condition.Condition;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {@JsonSubTypes.Type(name = "and", value = AndEventCondition.class),
    @JsonSubTypes.Type(name = "or", value = OrEventCondition.class),
    @JsonSubTypes.Type(name = "value_threshold", value = ValueEventCondition.class),})
public interface EventCondition extends Condition {
    @JsonIgnore
    EventConditionChecker getChecker();
}
