package com.elasticsearch.cloud.monitor.metric.common.aggregator;

import java.io.Serializable;

public interface Aggregator extends Serializable {

    void addValue(double value);

    double getValue();

}

