package com.elasticsearch.cloud.monitor.metric.common.aggregator;

import com.elasticsearch.cloud.monitor.metric.common.datapoint.AggregatableDataPoint;
import com.elasticsearch.cloud.monitor.metric.common.datapoint.DataPoint;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DataPointDownsampler implements DataPointAggregator<AggregatableDataPoint> {
    private static final Log LOG = LogFactory.getLog(DataPointDownsampler.class);

    private final String newName;
    private final long timestamp;
    private final String aggName;
    private final Map<Map<String, String>, AggregatableDataPoint> dataPoints;

    public DataPointDownsampler(String newName, long timestamp, String aggName) {
        this.newName = newName;
        this.timestamp = timestamp;
        this.aggName = aggName;
        this.dataPoints = new HashMap<>();
    }

    @Override
    public void addDataPoint(DataPoint dp) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("downsample <" + dp + ">");
        }
        // since every DataPoint has the same name, so we distinct DataPoints by its tags
        final Map<String, String> key = dp.getTags();
        AggregatableDataPoint aggDataPoint = dataPoints.get(key);
        if (aggDataPoint == null) {
            synchronized (dataPoints) {
                aggDataPoint = dataPoints.get(key);
                if (aggDataPoint == null) {
                    aggDataPoint = new AggregatableDataPoint(newName, this.timestamp, key, Aggregators.get(aggName));
                    dataPoints.put(key, aggDataPoint);
                }
            }
        }
        aggDataPoint.addDataPoint(dp);
    }

    @Override
    public Iterator<AggregatableDataPoint> iterator() {
        return dataPoints.values().iterator();
    }
}
