package com.elasticsearch.cloud.monitor.metric.common.datapoint;

import java.util.Map;

/**
 * @author xiaoping
 * @date 2019/12/3
 */
public class ImmutableMultiValueDataPoint implements MultiValueDataPoint {
    private final String name;
    private final long timestamp;
    private final String values;
    private final Map<String, String> tags;
    private String granularity;
    private String tenant;

    public ImmutableMultiValueDataPoint(final String name, final long timestamp, final Map<String, String> tags,
        final String values, String tenant) {
        this(name, timestamp, tags, null, values, tenant);
    }

    public ImmutableMultiValueDataPoint(final String name, final long timestamp, final Map<String, String> tags,
        String granularity, final String values, String tenant) {
        this.name = name;
        this.timestamp = timestamp;
        this.values = values;
        this.tags = tags;
        this.granularity = granularity;
        this.tenant = tenant;
    }

    @Override
    public String getTenant() {
        return tenant;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String getValues() {
        return values;
    }

    @Override
    public Map<String, String> getTags() {
        return tags;
    }

    @Override
    public String getGranularity() {
        return granularity;
    }
}
