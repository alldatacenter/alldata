package com.elasticsearch.cloud.monitor.metric.common.checker.duration;

import com.elasticsearch.cloud.monitor.metric.common.core.Alarm;
import com.elasticsearch.cloud.monitor.metric.common.core.AlarmLevel;
import com.elasticsearch.cloud.monitor.metric.common.datapoint.DataPoint;
import com.elasticsearch.cloud.monitor.metric.common.provider.DataProvider;
import com.elasticsearch.cloud.monitor.metric.common.rule.condition.duration.DurationCondition;
import com.elasticsearch.cloud.monitor.metric.common.rule.condition.duration.OrDurationCondition;
import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.List;

public class OrDurationConditionChecker extends CompositeDurationConditionChecker {
    private static final Joiner OR_JOINER = Joiner.on("或者");
    private final List<DurationConditionChecker> checkers;

    public OrDurationConditionChecker(final OrDurationCondition condition) {
        checkers = new ArrayList<>(condition.getConditions().size());
        for (DurationCondition c : condition.getConditions()) {
            checkers.add(c.getChecker());
        }
    }

    @Override
    public Alarm check(final DataPoint dp, final DataProvider dataProvider) {
        AlarmLevel maxLevel = AlarmLevel.WARNING;
        List<String> msgs = new ArrayList<>(checkers.size());
        boolean pass = false;
        boolean dataMissed = false;
        for (DurationConditionChecker checker : checkers) {
            Alarm alarm = checker.check(dp, dataProvider);
            if (alarm != null) {
                dataMissed |= alarm.getDataMissed();
            }
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
            return new Alarm(AlarmLevel.NORMAL, OR_JOINER.join(msgs), dataMissed);
        }
        if (msgs.size() > 1) {
            for (int i = 0; i < msgs.size(); ++i) {
                msgs.set(i, "[" + msgs.get(i) + "]");
            }
        }
        return new Alarm(maxLevel, OR_JOINER.join(msgs), dataMissed);
    }
}
