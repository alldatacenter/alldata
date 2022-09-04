package com.elasticsearch.cloud.monitor.metric.common.provider;

import com.elasticsearch.cloud.monitor.metric.common.datapoint.DataPoint;

import java.util.List;
import java.util.Map;

/**
 * Created by colin on 2017/4/6.
 */
public interface DataProvider {

    void put(DataPoint dp);

    List<Double> get(final long start, final long end, final Map<String, String> queryTags);
}
