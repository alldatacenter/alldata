package com.elasticsearch.cloud.monitor.metric.common.checker.duration;

import com.elasticsearch.cloud.monitor.metric.common.core.AlarmLevel;
import com.elasticsearch.cloud.monitor.metric.common.utils.TimeUtils;
import com.elasticsearch.cloud.monitor.metric.common.rule.condition.duration.RateChangeDurationCondition;
import com.google.common.base.Preconditions;

public class RateChangeDurationConditionChecker extends CompareDurationConditionChecker {

    public RateChangeDurationConditionChecker(final RateChangeDurationCondition condition) {
        super(condition);
    }

    @Override
    public double getChangeValue(final double curDurationValue,
                                 final double compareToDurationValue) {
        Preconditions.checkArgument(compareToDurationValue != 0, "dividend is 0");
        return ((curDurationValue - compareToDurationValue) / Math.abs(compareToDurationValue)) * 100;
    }

    @Override
    public String getAlarmMessage(final double curDurationValue,
                                  final double compareToDurationValue,
                                  final double changeValue, final AlarmLevel level) {
        StringBuilder sb = new StringBuilder();
        sb.append("当前");
        sb.append(TimeUtils.getDurationChineseName(condition.getDuration()));
        sb.append("的").append(isDurationGreaterThanCheckInterval() ? "平均值" : "值");
        sb.append(String.format("%.2f", curDurationValue));
        sb.append("比");
        sb.append(TimeUtils.getDurationChineseName(condition.getComparedTo()));
        sb.append("前的").append(isDurationGreaterThanCheckInterval() ? "平均值" : "值");
        sb.append(String.format("%.2f", compareToDurationValue));
        if (curDurationValue >= compareToDurationValue) {
            sb.append("增长了");
        } else {
            sb.append("降低了");
        }
        sb.append(String.format("%.2f", Math.abs(changeValue))).append("%");

        if (AlarmLevel.NORMAL.equals(level)){
            return sb.toString();
        }

        if (!condition.isMathAbs() && changeValue < 0) {
            sb.append(",").append(String.format("%.2f", changeValue)).append("%");
        }
        sb.append(condition.getComparator());
        sb.append("阈值");
        double threshold = condition.getThresholds().get(level);
        sb.append(String.format("%.2f", threshold)).append("%");
        return sb.toString();
    }

}
