package com.elasticsearch.cloud.monitor.metric.common.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * @author xiaoping
 * @date 2020/6/4
 */
@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchFilterConfig {
    /**
     * metric name 支持通配符匹配
     */
    private List<Filter> whiteFilters;
    private List<String> whiteMetric;
    private Map<String, Granularity> metricToGranularity;
    private Granularity defaultGranularity;

}
