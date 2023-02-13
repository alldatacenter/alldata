package com.elasticsearch.cloud.monitor.metric.common.datapoint;

import com.elasticsearch.cloud.monitor.metric.common.aggregator.Aggregator;
import com.elasticsearch.cloud.monitor.metric.common.utils.TagUtils;
import com.google.gson.JsonObject;

import java.util.Map;

public class AggregatableDataPoint implements DataPoint {
    private String name;
    private long timestamp;
    private Aggregator aggregator;
    private Map<String, String> tags;
    private String granularity;
    private int aggregateCount;

    public AggregatableDataPoint(final String name, final long timestamp, final Map<String, String> tags,
                                 final Aggregator aggregator) {
        this(name, timestamp, tags, null, aggregator);
    }

    public AggregatableDataPoint(final String name, final long timestamp, final Map<String, String> tags,
                                 final String granularity, final Aggregator aggregator) {
        this.name = name;
        this.tags = tags;
        this.timestamp = timestamp;
        this.aggregator = aggregator;
        this.granularity = granularity;
    }

    public synchronized void addDataPoint(DataPoint dp) {
        this.aggregator.addValue(dp.getValue());
        aggregateCount++;
    }

    public synchronized void aggregateDataPointValue(double value) {
        this.aggregator.addValue(value);
        aggregateCount++;
    }

    public int getAggregateCount() {
        return aggregateCount;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Map<String, String> getTags() {
        return this.tags;
    }

    @Override
    public String getGranularity() {
        return granularity;
    }

    @Override
    public JsonObject toJsonObject() {
        //todo
        return null;
    }

    @Override
    public long getTimestamp() {
        return this.timestamp;
    }

    @Override
    public double getValue() {
        return this.aggregator.getValue();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("name=").append(name);
        sb.append(" ts=").append(timestamp);
        sb.append(" value=").append(aggregator.getValue());
        if (tags != null && !tags.isEmpty()) {
            sb.append(" ").append(TagUtils.getTag(tags));
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof DataPoint)) {
            return false;
        }
        DataPoint dp = (DataPoint) obj;
        return name.equals(dp.getName()) && timestamp == dp.getTimestamp()
                && Math.abs(getValue() - dp.getValue()) < 1e-6 && tags.equals(dp.getTags());
    }
}
