package com.elasticsearch.cloud.monitor.metric.common.checker.duration;

import com.elasticsearch.cloud.monitor.metric.common.core.Alarm;
import com.elasticsearch.cloud.monitor.metric.common.core.AlarmLevel;
import com.elasticsearch.cloud.monitor.metric.common.datapoint.DataPoint;
import com.elasticsearch.cloud.monitor.metric.common.provider.DataProvider;
import com.elasticsearch.cloud.monitor.metric.common.rule.condition.duration.AndDurationCondition;
import com.elasticsearch.cloud.monitor.metric.common.rule.condition.duration.DurationCondition;
import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.List;

public class AndDurationConditionChecker extends CompositeDurationConditionChecker {
    private static final Joiner AND_JOINER = Joiner.on("并且");
    private final List<DurationConditionChecker> checkers;

    public AndDurationConditionChecker(final AndDurationCondition condition) {
        checkers = new ArrayList<>(condition.getConditions().size());
        for (DurationCondition c : condition.getConditions()) {
            checkers.add(c.getChecker());
        }
    }

    @Override
    public Alarm check(final DataPoint dp, DataProvider dataProvider) {
        AlarmLevel minLevel = AlarmLevel.CRITICAL;
        List<String> msgs = new ArrayList<>(checkers.size());
        boolean pass = true;
        boolean dataMissed = false;
        for (DurationConditionChecker checker : checkers) {
            Alarm alarm = checker.check(dp, dataProvider);
            if (alarm != null) {
                dataMissed |= alarm.getDataMissed();
            }
            if (alarm == null) {
                pass = false;
            } else {
                if(alarm.isOk()){
                    pass = false;
                }
                msgs.add(alarm.getMsg());
                if (alarm.getLevel().getValue() < minLevel.getValue()) {
                    minLevel = alarm.getLevel();
                }
            }
        }
        if (!pass) {
            return new Alarm(AlarmLevel.NORMAL, AND_JOINER.join(msgs), dataMissed);
        }
        if (msgs.size() > 1) {
            for (int i = 0; i < msgs.size(); ++i) {
                msgs.set(i, "[" + msgs.get(i) + "]");
            }
        }
        return new Alarm(minLevel, AND_JOINER.join(msgs), dataMissed);
    }
}
