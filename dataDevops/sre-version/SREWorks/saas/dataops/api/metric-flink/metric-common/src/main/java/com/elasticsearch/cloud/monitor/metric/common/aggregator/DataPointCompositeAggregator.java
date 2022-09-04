package com.elasticsearch.cloud.monitor.metric.common.aggregator;

import com.elasticsearch.cloud.monitor.metric.common.datapoint.DataPoint;
import com.google.common.base.Preconditions;

import java.util.Iterator;

/**
 * 一次性的组合聚合器，在使用iterator遍历DataPoint后将不能继续添加DataPoint
 */
public class DataPointCompositeAggregator implements DataPointAggregator<DataPoint> {

    private final DataPointAggregator<DataPoint>[] aggregators;
    private volatile boolean hasIterated;

    public DataPointCompositeAggregator(DataPointAggregator... aggregators) {
        Preconditions.checkArgument(aggregators.length > 0, "no aggregators");
        this.aggregators = aggregators;
        this.hasIterated = false;
    }

    @Override
    public void addDataPoint(DataPoint dp) {
        Preconditions.checkState(!hasIterated, "can not addDataPoint after iterator");
        aggregators[0].addDataPoint(dp);
    }

    @Override
    public synchronized Iterator<DataPoint> iterator() {
        int size = aggregators.length;
        // 让DataPoint在所有Aggregator中依次做一次聚合，最后的数据将从最后一个Aggregator中流出
        // 在调用了此方法后，将不能再addDataPoint
        if (hasIterated) {
            return aggregators[size - 1].iterator();
        }
        hasIterated = true;
        for (int i = 1; i < size; ++i) {
            for (DataPoint dp : aggregators[i - 1]) {
                aggregators[i].addDataPoint(dp);
            }
        }
        return aggregators[size - 1].iterator();
    }

}
