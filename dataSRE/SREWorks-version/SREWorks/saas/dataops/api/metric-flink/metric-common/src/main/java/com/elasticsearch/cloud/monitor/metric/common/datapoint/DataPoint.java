package com.elasticsearch.cloud.monitor.metric.common.datapoint;

import com.google.gson.JsonObject;

import java.io.Serializable;
import java.util.Map;

public interface DataPoint extends Serializable {
    default String getTenant() {
        return "";
    }

    String getName();

    long getTimestamp();

    double getValue();

    Map<String, String> getTags();

    String getGranularity();

    JsonObject toJsonObject();
}
