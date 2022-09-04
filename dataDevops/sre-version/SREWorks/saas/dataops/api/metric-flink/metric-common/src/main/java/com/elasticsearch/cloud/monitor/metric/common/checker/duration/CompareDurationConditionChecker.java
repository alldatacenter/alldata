package com.elasticsearch.cloud.monitor.metric.common.checker.duration;

import com.elasticsearch.cloud.monitor.metric.common.aggregator.Aggregator;
import com.elasticsearch.cloud.monitor.metric.common.aggregator.Aggregators;
import com.elasticsearch.cloud.monitor.metric.common.core.Alarm;
import com.elasticsearch.cloud.monitor.metric.common.core.AlarmLevel;
import com.elasticsearch.cloud.monitor.metric.common.core.Constants;
import com.elasticsearch.cloud.monitor.metric.common.datapoint.DataPoint;
import com.elasticsearch.cloud.monitor.metric.common.provider.DataProvider;
import com.elasticsearch.cloud.monitor.metric.common.rule.condition.duration.CompareDurationCondition;
import com.elasticsearch.cloud.monitor.metric.common.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public abstract class CompareDurationConditionChecker extends SingleDurationConditionChecker {
    private final long duration;
    private final long comparedTo;
    protected final CompareDurationCondition condition;

    public CompareDurationConditionChecker(final CompareDurationCondition condition) {
        this.condition = condition;
        this.duration = TimeUtils.parseDuration(condition.getDuration());
        this.comparedTo = TimeUtils.parseDuration(condition.getComparedTo());
    }

    // TODO no check at first time
    @Override
    public final Alarm check(final DataPoint dp, final DataProvider dataProvider) {
        Double compareToDurationValue = getDurationValue(dataProvider, dp, comparedTo);
        if (compareToDurationValue == null) {
            log.error("get duration value failed, duration:{}, dp :{}", comparedTo, dp.toString());
            return new Alarm(AlarmLevel.NORMAL, "", true);
        }
        Double curDurationValue = getDurationValue(dataProvider, dp, 0);
        if (curDurationValue == null) {
            log.error("get duration value failed, duration:{}, dp :{}", 0, dp.toString());
            return new Alarm(AlarmLevel.NORMAL, "", true);
        }
        double changeValue = getChangeValue(curDurationValue, compareToDurationValue);
        AlarmLevel level = condition.check(changeValue);
        if (level != null) {
            String msg = getAlarmMessage(curDurationValue, compareToDurationValue, changeValue, level);
            return new Alarm(level, msg);
        } else {
            return null;
        }
    }

    protected boolean isDurationGreaterThanCheckInterval() {
        return duration > Constants.CHECK_INTERVAL;
    }

    protected abstract double getChangeValue(final double curDurationValue,
                                             final double compareToDurationValue);

    protected abstract String getAlarmMessage(final double curDurationValue,
                                              final double compareToDurationValue,
                                              final double changeValue, final AlarmLevel level);

    private Double getDurationValue(final DataProvider dataProvider, final DataPoint dp,
                                    final long comparedTo) {
        long end = TimeUtils.toMillisecond(dp.getTimestamp()) - comparedTo;
        long start = end + Constants.CHECK_INTERVAL - duration;

        List<Double> values = dataProvider.get(start, end, dp.getTags());
        if (values.isEmpty()) {
            return null;
        }
        Aggregator avg = Aggregators.get("avg");
        for (double value : values) {
            avg.addValue(value);
        }
        return avg.getValue();
    }

}
