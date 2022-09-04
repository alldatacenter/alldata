package com.elasticsearch.cloud.monitor.metric.common.core;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Map;

@Setter
@Getter
public class MetricAlarm implements Serializable {

    private Alarm alarm;
    private long timestamp;
    private long ruleId;
    private Map<String, String> tags;
    private boolean error;
    private String metric;

}
