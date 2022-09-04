package com.elasticsearch.cloud.monitor.metric.common.checker.duration;

import com.elasticsearch.cloud.monitor.metric.common.core.AlarmLevel;
import com.elasticsearch.cloud.monitor.metric.common.rule.condition.duration.ChangeDurationCondition;
import com.elasticsearch.cloud.monitor.metric.common.utils.TimeUtils;

public class ChangeDurationConditionChecker extends CompareDurationConditionChecker {

    public ChangeDurationConditionChecker(final ChangeDurationCondition condition) {
        super(condition);
    }

    @Override
    protected double getChangeValue(final double curDurationValue,
                                 final double compareToDurationValue) {
        return curDurationValue - compareToDurationValue;
    }

    @Override
    protected String getAlarmMessage(final double curDurationValue,
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
            sb.append("增加了");
        } else {
            sb.append("减少了");
        }
        sb.append(String.format("%.2f", Math.abs(changeValue)));

        if(AlarmLevel.NORMAL.equals(level)){
            return  sb.toString();
        }

        if (!condition.isMathAbs() && changeValue < 0) {
            sb.append(",").append(String.format("%.2f", changeValue));
        }
        sb.append(condition.getComparator());
        sb.append("阈值");
        double threshold = condition.getThresholds().get(level);
        sb.append(String.format("%.2f", threshold));
        return sb.toString();
    }

}
