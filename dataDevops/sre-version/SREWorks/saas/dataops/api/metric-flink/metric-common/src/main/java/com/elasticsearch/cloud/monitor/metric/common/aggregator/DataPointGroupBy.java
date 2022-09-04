package com.elasticsearch.cloud.monitor.metric.common.aggregator;

import com.elasticsearch.cloud.monitor.metric.common.datapoint.AggregatableDataPoint;
import com.elasticsearch.cloud.monitor.metric.common.datapoint.DataPoint;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DataPointGroupBy implements DataPointAggregator<AggregatableDataPoint> {
    private static final Log LOG = LogFactory.getLog(DataPointGroupBy.class);

    private final long timestamp;
    private final String aggName;
    private final List<String> groupBys;
    private final Map<Map<String, String>, AggregatableDataPoint> dataPoints;

    public DataPointGroupBy(long timestamp, String aggName, List<String> groupBys) {
        this.timestamp = timestamp;
        this.aggName = aggName;
        this.groupBys = groupBys;
        this.dataPoints = new HashMap<>();
    }

    @Override
    public void addDataPoint(DataPoint dp) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("groupby <" + dp + ">");
        }
        final Map<String, String> groupByTags = getGroupByTags(dp);
        AggregatableDataPoint aggDataPoint = dataPoints.get(groupByTags);
        if (aggDataPoint == null) {
            synchronized (dataPoints) {
                aggDataPoint = dataPoints.get(groupByTags);
                if (aggDataPoint == null) {
                    aggDataPoint =
                        new AggregatableDataPoint(dp.getName(), timestamp, groupByTags, Aggregators.get(aggName));
                    dataPoints.put(groupByTags, aggDataPoint);
                }
            }
        }
        aggDataPoint.addDataPoint(dp);
    }

    private Map<String, String> getGroupByTags(DataPoint dp) {
        if (groupBys.isEmpty()) {
            return dp.getTags();
        }
        Map<String, String> tags = new HashMap<>(dp.getTags().size());
        for (String groupBy : groupBys) {
            if (dp.getTags().containsKey(groupBy)) {
                tags.put(groupBy, dp.getTags().get(groupBy));
            }
        }
        return tags;
    }

    @Override
    public Iterator<AggregatableDataPoint> iterator() {
        return dataPoints.values().iterator();
    }
}
