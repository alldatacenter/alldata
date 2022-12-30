package com.elasticsearch.cloud.monitor.metric.common.checker.duration;

import com.elasticsearch.cloud.monitor.metric.common.aggregator.Aggregator;
import com.elasticsearch.cloud.monitor.metric.common.aggregator.Aggregators;
import com.elasticsearch.cloud.monitor.metric.common.core.Alarm;
import com.elasticsearch.cloud.monitor.metric.common.core.AlarmLevel;
import com.elasticsearch.cloud.monitor.metric.common.core.Constants;
import com.elasticsearch.cloud.monitor.metric.common.datapoint.DataPoint;
import com.elasticsearch.cloud.monitor.metric.common.provider.DataProvider;
import com.elasticsearch.cloud.monitor.metric.common.rule.condition.duration.ValueDurationCondition;
import com.elasticsearch.cloud.monitor.metric.common.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ValueDurationConditionChecker extends SingleDurationConditionChecker {
    private final long duration;
    private final ValueDurationCondition condition;

    public ValueDurationConditionChecker(final ValueDurationCondition condition) {
        this.condition = condition;
        this.duration = TimeUtils.parseDuration(condition.getDuration());
    }

    // TODO no check at first time
    @Override
    public Alarm check(final DataPoint dp, final DataProvider dataProvider) {
        long dpTs = TimeUtils.toMillisecond(dp.getTimestamp());
        long start = dpTs + Constants.CHECK_INTERVAL - duration;

        List<Double> values = dataProvider.get(start, dpTs, dp.getTags());
        if (values.isEmpty()) {
            log.error("ValueDurationConditionChecker,{},{},{}", start, dpTs, dp);
            return null;
        }
        Aggregator avg = Aggregators.get("avg");
        for (double value : values) {
            avg.addValue(value);
        }
        double value = avg.getValue();
        AlarmLevel level = condition.check(value);
        if (level != null) {
            return new Alarm(level, getMsg(value, level));
        } else {
            return null;
        }
    }

    private String getMsg(final double value, final AlarmLevel level) {

        StringBuilder sb = new StringBuilder();
        sb.append("当前");
        sb.append(TimeUtils.getDurationChineseName(condition.getDuration()));
        sb.append("的").append(duration > Constants.CHECK_INTERVAL ? "平均值" : "值");
        double abs = condition.isMathAbs() ? Math.abs(value) : value;

        /**
          need to be here. or will NPE.
         */
        if(AlarmLevel.NORMAL.equals(level)){
            sb.append(String.format("%s", abs));
            return sb.toString();
        }
        double threshold = condition.getThresholds().get(level);

        if(Math.abs(abs) - threshold >= 0.01d) {
            sb.append(String.format("%.2f", abs ));
        }else{
            sb.append(String.format("%s", abs));
        }
        sb.append(condition.getComparator());
        sb.append("阈值");
        sb.append(String.format("%.2f", threshold));

        return sb.toString();
    }

}
