package com.elasticsearch.cloud.monitor.metric.common.datapoint;

import com.elasticsearch.cloud.monitor.metric.common.utils.TagUtils;
import com.elasticsearch.cloud.monitor.metric.common.utils.TimeUtils;
import com.elasticsearch.cloud.monitor.metric.common.utils.Utils;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImmutableDataPoint implements DataPoint {
    private String name;
    private long timestamp;
    private Double value;
    private Map<String, String> tags;
    private String granularity;
    private String tenant = "";
    private List<String> metrics;
    private List<Double> values;

    public ImmutableDataPoint(final String name, final long timestamp, final double value,
        final Map<String, String> tags) {
        this(name, timestamp, value, tags, null);
    }

    public ImmutableDataPoint(final String name, final long timestamp, final double value,
        final Map<String, String> tags, String granularity) {
        this.name = name;
        this.timestamp = timestamp;
        this.value = value;
        this.tags = tags;
        this.granularity = granularity;
    }

    public ImmutableDataPoint(List<String> metrics, List<Double> values, long timestamp, Map<String, String> tags,
        String granularity) {
        this.metrics = metrics;
        this.values = values;
        this.timestamp = timestamp;
        this.tags = tags;
        this.granularity = granularity;
    }

    @Setter
    @Getter
    @NoArgsConstructor
    public static class MetricAndTimestamp {
        private long timestamp;
        private String metric;

        public MetricAndTimestamp(long timestamp, String metric) {
            this.timestamp = timestamp;
            this.metric = metric;
        }
    }

    public static MetricAndTimestamp getTimestamp(String str) throws Exception {
        Preconditions.checkArgument(StringUtils.isNotBlank(str), "empty data point");
        String[] words = Utils.splitString(str.trim(), ' ');
        Preconditions.checkArgument(words.length >= 2, "[1] illegal data point: " + str);
        String metric = words[0];
        long timestamp;
        try {
            timestamp = TagUtils.parseLong(words[1].replace(".", ""));
        } catch (Exception ex) {
            throw new Exception(String.format("invalid timestamp, raw content is %s", str), ex);
        }
        timestamp = TimeUtils.toMillisecond(timestamp);
        return new MetricAndTimestamp(timestamp, metric);
    }

    public static ImmutableDataPoint from(String str) throws Exception {
        Preconditions.checkArgument(StringUtils.isNotBlank(str), "empty data point");
        String[] words = Utils.splitString(str.trim(), ' ');
        Preconditions.checkArgument(words.length >= 2, "[1] illegal data point: " + str);
        String name = words[0];
        int newFields = 0;
        long timestamp;
        try {
            timestamp = TagUtils.parseLong(words[1].replace(".", ""));
        } catch (Exception ex) {
            throw new Exception(String.format("invalid timestamp, raw content is %s", str), ex);
        }
        timestamp = TimeUtils.toMillisecond(timestamp);
        Preconditions.checkArgument(words.length >= 3 + newFields,
            "[2] illegal data point: " + str);
        String valueString = words[2 + newFields];
        double value;
        try {
            value = parseValue(valueString);
        } catch (Exception e) {
            throw new Exception(String.format("invalid value, raw string is %s", str), e);
        }
        List<String> metrics = null;
        List<Double> values = null;
        int i = 3 + newFields;
        for (; i < words.length - 1; i += 2) {
            if (org.apache.commons.lang.StringUtils.isNotEmpty(words[i]) && !words[i].contains("=")) {
                if (metrics == null) {
                    metrics = Lists.newArrayList();
                    values = Lists.newArrayList();
                }
                values.add(parseValue(words[i]));
                metrics.add(words[i + 1]);
            } else {
                break;
            }
        }
        final Map<String, String> tags = new HashMap<>();
        for (; i < words.length; i++) {
            if (!words[i].isEmpty()) {
                try {
                    TagUtils.parse(tags, words[i]);
                } catch (Exception e) {
                    throw new Exception(String.format("invalid tag, content is %s", str), e);
                }
            }
        }
        if (metrics != null) {
            metrics.add(name);
            values.add(value);
            return new ImmutableDataPoint(metrics, values, timestamp, tags, null);
        } else {
            return new ImmutableDataPoint(name, timestamp, value, tags);
        }
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    private static double parseValue(String valueString) {
        if ("false".equalsIgnoreCase(valueString)) {
            return 0;
        } else if ("true".equalsIgnoreCase(valueString)) {
            return 1;
        } else {
            if (valueString.contains("_")) {
                valueString = valueString.split("_")[0];
            }
            double value = Double.parseDouble(valueString);
            if (value == Double.MIN_VALUE) {
                return 0;
            } else {
                return value;
            }
        }
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
    public double getValue() {
        return value;
    }

    public List<String> getMetrics() {
        return metrics;
    }

    public List<Double> getValues() {
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

    @Override
    public JsonObject toJsonObject() {
        JsonObject tgs = new JsonObject();
        for (String k : tags.keySet()) {
            tgs.addProperty(k, tags.get(k));
        }

        JsonObject o = new JsonObject();
        o.addProperty("metric_name", name);
        o.addProperty("granularity", granularity);
        o.addProperty("timestamp", timestamp);
        o.addProperty("metric_value", value);
        o.add("tags", tgs);
        return o;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        ImmutableDataPoint that = (ImmutableDataPoint)o;
        return timestamp == that.timestamp &&
            Objects.equal(name, that.name) &&
            Objects.equal(value, that.value) &&
            Objects.equal(tags, that.tags) &&
            Objects.equal(granularity, that.granularity) &&
            Objects.equal(tenant, that.tenant) &&
            Objects.equal(metrics, that.metrics) &&
            Objects.equal(values, that.values);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("name=").append(name);
        sb.append(" ts=").append(timestamp);
        sb.append(" value=").append(value);
        if (tags != null && !tags.isEmpty()) {
            sb.append(" ").append(TagUtils.getTag(tags));
        }
        return sb.toString();
    }
}
