package com.elasticsearch.cloud.monitor.metric.common.aggregator;


import com.elasticsearch.cloud.monitor.metric.common.datapoint.DataPoint;

public interface DataPointAggregator<D extends DataPoint> extends Iterable<D> {

    void addDataPoint(DataPoint dp);
}
