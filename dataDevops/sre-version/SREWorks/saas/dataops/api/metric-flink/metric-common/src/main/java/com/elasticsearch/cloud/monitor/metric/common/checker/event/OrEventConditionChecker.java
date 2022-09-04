package com.elasticsearch.cloud.monitor.metric.common.checker.event;

import com.elasticsearch.cloud.monitor.metric.common.core.Alarm;
import com.elasticsearch.cloud.monitor.metric.common.core.AlarmLevel;
import com.elasticsearch.cloud.monitor.metric.common.datapoint.DataPoint;
import com.elasticsearch.cloud.monitor.metric.common.rule.condition.event.EventCondition;
import com.elasticsearch.cloud.monitor.metric.common.rule.condition.event.OrEventCondition;
import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.List;

public class OrEventConditionChecker implements EventConditionChecker {
    private static final Joiner OR_JOINER = Joiner.on("或者");
    private final List<EventConditionChecker> checkers;

    public OrEventConditionChecker(final OrEventCondition condition) {
        checkers = new ArrayList<>(condition.getConditions().size());
        for (EventCondition c : condition.getConditions()) {
            checkers.add(c.getChecker());
        }
    }

    @Override
    public Alarm check(final DataPoint dp) {
        AlarmLevel maxLevel = AlarmLevel.WARNING;
        List<String> msgs = new ArrayList<>(checkers.size());
        boolean pass = false;
        for (EventConditionChecker checker : checkers) {
            Alarm alarm = checker.check(dp);
            if (alarm == null || alarm.isOk()) {
                if(alarm != null && msgs.size() == 0){
                    msgs.add(alarm.getMsg());
                }
                continue;
            }
            pass = true;
            msgs.add(alarm.getMsg());
            if (alarm.getLevel().getValue() > maxLevel.getValue()) {
                maxLevel = alarm.getLevel();
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
        return new Alarm(maxLevel, OR_JOINER.join(msgs));
    }
}
