package com.elasticsearch.cloud.monitor.metric.common.rule;

import com.elasticsearch.cloud.monitor.metric.common.rule.filter.TagVFilter;
import com.elasticsearch.cloud.monitor.metric.common.utils.TagUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 子查询抽象类
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2022/06/11 13:23
 */
public abstract class SubQuery {

    @JsonProperty("metric")
    protected String metric;

    @JsonProperty("tags")
    protected Map<String, String> tags;

    @JsonProperty("aggregator")
    protected  String aggregator;

    @JsonProperty("ds-aggregator")
    protected String dsAggregator;

    @JsonProperty("rate")
    protected boolean rate;

    public boolean isRate() {
        return rate;
    }

    public void setRate(boolean rate) {
        this.rate = rate;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;

        if (tagVFilters == null) {
            tagVFilters = new LinkedList<>();
            TagVFilter.tagsToFilters(tags, tagVFilters);
        }
        for (TagVFilter tagVFilter : tagVFilters) {
            tagVFilterMap.put(tagVFilter.getTagk(), tagVFilter);
            if (tagVFilter.isGroupBy()) {
                groupByFilterMap.put(tagVFilter.getTagk(), tagVFilter);
            }
        }
    }

    public String getAggregator() {
        return aggregator;
    }

    public void setAggregator(String aggregator) {
        this.aggregator = aggregator;
    }

    public String getDsAggregator() {
        return dsAggregator;
    }

    public void setDsAggregator(String dsAggregator) {
        this.dsAggregator = dsAggregator;
    }

    @JsonIgnore
    protected List<TagVFilter> tagVFilters;
    @JsonIgnore
    protected Map<String, TagVFilter> tagVFilterMap = new HashMap<>();
    @JsonIgnore
    protected Map<String, TagVFilter> groupByFilterMap = new HashMap<>();

    @JsonIgnore
    public List<TagVFilter> getTagVFilters() {
        return tagVFilters;
    }

    @JsonIgnore
    public Map<String, TagVFilter> getFilterMap() {
        return tagVFilterMap;
    }

    @JsonIgnore
    public Map<String, TagVFilter> getGroupByFilters() {
        return groupByFilterMap;
    }

    @JsonIgnore
    public String getAggregateId() {
        return getQueryId() + "-" +  dsAggregator + "-" +  aggregator +  "-" +rate;
    }

    @JsonIgnore
    public String getAggregateInfo() {
        return  metric +  dsAggregator+ "|" + aggregator + TagUtils.getTag(tags) + rate;
    }

    @JsonIgnore
    public abstract String  getQueryId();

    @JsonIgnore
    public abstract Long getParentId();

    public void setTagVFilters(List<TagVFilter> tagVFilters) {
        this.tagVFilters = tagVFilters;
    }

    public Map<String, TagVFilter> getTagVFilterMap() {
        return tagVFilterMap;
    }

    public void setTagVFilterMap(Map<String, TagVFilter> tagVFilterMap) {
        this.tagVFilterMap = tagVFilterMap;
    }

    public Map<String, TagVFilter> getGroupByFilterMap() {
        return groupByFilterMap;
    }

    public void setGroupByFilterMap(Map<String, TagVFilter> groupByFilterMap) {
        this.groupByFilterMap = groupByFilterMap;
    }
}
