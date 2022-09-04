package com.elasticsearch.cloud.monitor.metric.common.checker.event;

import com.elasticsearch.cloud.monitor.metric.common.core.Alarm;
import com.elasticsearch.cloud.monitor.metric.common.core.AlarmLevel;
import com.elasticsearch.cloud.monitor.metric.common.datapoint.DataPoint;
import com.elasticsearch.cloud.monitor.metric.common.rule.condition.event.AndEventCondition;
import com.elasticsearch.cloud.monitor.metric.common.rule.condition.event.EventCondition;
import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.List;

public class AndEventConditionChecker implements EventConditionChecker {
    private static final Joiner AND_JOINER = Joiner.on("并且");
    private final List<EventConditionChecker> checkers;

    public AndEventConditionChecker(final AndEventCondition condition) {
        checkers = new ArrayList<>(condition.getConditions().size());
        for (EventCondition c : condition.getConditions()) {
            checkers.add(c.getChecker());
        }
    }

    @Override
    public Alarm check(final DataPoint dp) {
        AlarmLevel minLevel = AlarmLevel.CRITICAL;
        List<String> msgs = new ArrayList<>(checkers.size());
        boolean pass = true;
        for (EventConditionChecker checker : checkers) {
            Alarm alarm = checker.check(dp);
            if (alarm == null) {
                pass = false;
            } else {
                msgs.add(alarm.getMsg());
                if (alarm.getLevel().getValue() < minLevel.getValue()) {
                    minLevel = alarm.getLevel();
                }
            }
        }
        if (!pass) {
            return null;
        }
        if (msgs.size() > 1) {
            for (int i = 0; i < msgs.size(); ++i) {
                msgs.set(i, "[" + msgs.get(i) + "]");
            }
        }
        return new Alarm(minLevel, AND_JOINER.join(msgs));
    }
}

