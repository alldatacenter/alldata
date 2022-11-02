package com.elasticsearch.cloud.monitor.metric.common.rule.condition;

import com.elasticsearch.cloud.monitor.metric.common.checker.ConditionChecker;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;
import java.util.Objects;

public abstract class JoinCondition<T extends Condition> implements CompositeCondition<T> {
    public static final Joiner AND_JOINER = Joiner.on(" && ");
    public static final Joiner OR_JOINER = Joiner.on(" || ");
    private final List<T> conditions;
    private final Joiner joiner;

    public JoinCondition(List<T> conditions, Joiner joiner) {
        this.conditions = conditions;
        this.joiner = joiner;
    }

    @Override
    public void validate() throws Exception {
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(conditions),
            getClass().getSimpleName() + " must not be empty");
        for (T condition : conditions) {
            condition.validate();
        }
    }

    public abstract ConditionChecker getChecker();

    @JsonProperty("conditions")
    public List<T> getConditions() {
        return conditions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return Objects.equals(conditions, ((JoinCondition)o).conditions);
    }

    @Override
    public int hashCode() {
        return conditions != null ? conditions.hashCode() : 0;
    }

    @Override
    public String toString() {
        return String.format("(%s)", joiner.join(conditions));
    }
}
