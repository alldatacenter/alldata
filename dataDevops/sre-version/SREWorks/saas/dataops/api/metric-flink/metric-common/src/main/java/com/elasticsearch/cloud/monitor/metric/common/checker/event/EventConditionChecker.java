package com.elasticsearch.cloud.monitor.metric.common.checker.event;

import com.elasticsearch.cloud.monitor.metric.common.checker.ConditionChecker;
import com.elasticsearch.cloud.monitor.metric.common.core.Alarm;
import com.elasticsearch.cloud.monitor.metric.common.datapoint.DataPoint;

public interface EventConditionChecker extends ConditionChecker {
        Alarm check(final DataPoint dp);
}
