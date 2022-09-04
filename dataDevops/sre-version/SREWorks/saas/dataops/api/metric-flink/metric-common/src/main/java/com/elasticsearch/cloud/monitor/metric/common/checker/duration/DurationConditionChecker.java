package com.elasticsearch.cloud.monitor.metric.common.checker.duration;

import com.elasticsearch.cloud.monitor.metric.common.datapoint.DataPoint;
import com.elasticsearch.cloud.monitor.metric.common.checker.ConditionChecker;
import com.elasticsearch.cloud.monitor.metric.common.core.Alarm;
import com.elasticsearch.cloud.monitor.metric.common.provider.DataProvider;

public interface DurationConditionChecker extends ConditionChecker {

    Alarm check(final DataPoint dp, final DataProvider dataProvider);

}
