package org.apache.griffin.core.metric.model;

import lombok.Data;

@Data
public class MetricValueJson {

    private String name;

    private Long tmst;

    private String metaJson;

    private String valueJson;


    public MetricValueJson() {}

    public MetricValueJson(String name, Long tmst, String metaJson, String valueJson) {
        this.name = name;
        this.tmst = tmst;
        this.metaJson = metaJson;
        this.valueJson = valueJson;
    }
}
