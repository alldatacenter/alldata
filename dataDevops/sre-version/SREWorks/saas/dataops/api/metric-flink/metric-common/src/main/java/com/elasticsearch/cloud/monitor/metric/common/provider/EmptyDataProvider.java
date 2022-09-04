package com.elasticsearch.cloud.monitor.metric.common.provider;

import com.elasticsearch.cloud.monitor.metric.common.datapoint.DataPoint;
import java.util.List;
import java.util.Map;

public class EmptyDataProvider implements DataProvider{

    @Override
    public void put(DataPoint dp) {
    }

    @Override
    public List<Double> get(long start, long end, Map<String, String> queryTags) {
        return null;
    }
}
