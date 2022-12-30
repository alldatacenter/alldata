package com.elasticsearch.cloud.monitor.metric.common.rule;

import com.elasticsearch.cloud.monitor.metric.common.aggregator.Aggregators;
import com.elasticsearch.cloud.monitor.metric.common.datapoint.DataPoint;
import com.elasticsearch.cloud.monitor.metric.common.datapoint.ImmutableDataPoint;
import com.elasticsearch.cloud.monitor.metric.common.rule.compose.MonitorCompose;
import com.elasticsearch.cloud.monitor.metric.common.rule.condition.duration.DurationCondition;
import com.elasticsearch.cloud.monitor.metric.common.rule.condition.event.EventCondition;
import com.elasticsearch.cloud.monitor.metric.common.rule.condition.nodata.NoDataCondition;
import com.elasticsearch.cloud.monitor.metric.common.rule.expression.MetricCompose;
import com.elasticsearch.cloud.monitor.metric.common.rule.expression.SelectedMetric;
import com.elasticsearch.cloud.monitor.metric.common.rule.filter.TagVFilter;
import com.elasticsearch.cloud.monitor.metric.common.utils.ExpressionFactory;
import com.elasticsearch.cloud.monitor.metric.common.utils.FilterUtils;
import com.elasticsearch.cloud.monitor.metric.common.utils.MapUtils;
import com.elasticsearch.cloud.monitor.metric.common.utils.Utils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;

/**
 * 规则类
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2022/06/11 13:46
 */
public class Rule extends SubQuery implements Serializable {
    private static Logger LOG = LoggerFactory.getLogger(Rule.class);
    private Long id;
    private String uid;
    private String group;
    private String name;
    private String appName;
    private String detectType;
    private Map<String, Object> params;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private DurationCondition durationCondition;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private NoDataCondition noDataCondition;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private EventCondition eventCondition;

    private String durationConditionId;
    private String noDataConditionId;

    /**
     * 0-normal
     * 1-wildcard
     * 2-metric_compose_single_series
     * 3-metric_compose_multi_series
     */
    @JsonIgnore
    private int type;
    @JsonIgnore
    private String prefix;
    @JsonIgnore
    private String suffix;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MetricCompose metricCompose;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MonitorCompose monitorCompose;

    @JsonIgnore
    private boolean compose = false;

    @JsonIgnore
    public boolean isCompose() {
        return compose;
    }

    public Rule() {}

    @JsonCreator
    public Rule(@JsonProperty("id") Long id, @JsonProperty("group") String group, @JsonProperty("name") String name,
                @JsonProperty("detect_type") String detectType, @JsonProperty("params") Map<String, Object> params,
                @JsonProperty("app_name") String appName, @JsonProperty("metric") String metric,
                @JsonProperty("filters") List<TagVFilter> tagVFilters, @JsonProperty("tags") Map<String, String> tags,
                @JsonProperty("rate") boolean rate, @JsonProperty("aggregator") String aggregator,
                @JsonProperty("condition") DurationCondition durationCondition,
                @JsonProperty("no_data_condition") NoDataCondition noDataCondition,
                @JsonProperty("event_condition") EventCondition eventCondition) {
        this.id = id;
        this.group = group;
        this.name = name;
        this.appName = appName;
        this.detectType = detectType;
        this.params = params;
        this.metric = metric;
        this.tagVFilters = tagVFilters;
        this.tags = (tags == null) ? MapUtils.<String, String>emptyHashMap() : tags;
        this.rate = rate;
        this.aggregator = aggregator;
        this.durationCondition = durationCondition;
        this.noDataCondition = noDataCondition;
        this.eventCondition = eventCondition;
    }

    public Rule(Long  id, String group, String name, String appName, String metric, Map<String, String> tags,
                boolean rate, String aggregator, DurationCondition durationCondition, NoDataCondition noDataCondition) {
        this(id, group, name, "rule", null, appName, metric, null, tags, rate, aggregator,
                durationCondition, noDataCondition, null);
    }

    public Rule(Integer  id, String group, String name, String appName, String metric, Map<String, String> tags,
                boolean rate, String aggregator, DurationCondition durationCondition, NoDataCondition noDataCondition) {
        this(Long.valueOf(id), group, name, "rule", null, appName, metric, null, tags, rate, aggregator,
                durationCondition, noDataCondition, null);
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    private void metricValidate() throws Exception {
        if (metricCompose == null) {
            Preconditions.checkArgument(StringUtils.isNotEmpty(metric), "metric or metric_compose must not be empty");

            List<TagVFilter> tagsFilterList;
            if (tagVFilters == null) {
                tagsFilterList = new LinkedList<>();
                TagVFilter.tagsToFilters(tags, tagsFilterList);
            } else {
                tagsFilterList = tagVFilters;
            }
            for (TagVFilter tagVFilter : tagsFilterList) {
                tagVFilterMap.put(tagVFilter.getTagk(), tagVFilter);
                if (tagVFilter.isGroupBy()) {
                    groupByFilterMap.put(tagVFilter.getTagk(), tagVFilter);
                }
            }

            //wildcard
            if (StringUtils.isNotEmpty(metric) && metric.contains("*")) {
                type = 1; //suffix
                String[] components = Utils.splitString(metric, '*');
                Preconditions.checkArgument(components.length == 2, "wildcard metric must has prefix and suffix");
                //TODO need more check
                prefix = components[0];
                suffix = components[1];
            }
        } else {
            Preconditions.checkArgument(StringUtils.isNotEmpty(metricCompose.getExpression()),
                    "metric_compose expression must not be empty");
            List<SelectedMetric> metrics = metricCompose.getMetrics();
            Preconditions.checkArgument(metrics != null && metrics.size() > 0,
                    "metric_compose contain metrics must size > 0");

        }

        if (durationCondition != null || StringUtils.isNotBlank(detectType)) {
            if (metricCompose == null) {
                Preconditions.checkArgument(StringUtils.isNotEmpty(aggregator), "aggregator must not be empty");
                Aggregators.get(aggregator);
            } else {
                compose = true;
                List<SelectedMetric> metrics = metricCompose.getMetrics();
                boolean isMulti = false;
                boolean isSame = false;
                String lastTagVFilters = null;
                for (SelectedMetric selectedMetric : metrics) {
                    selectedMetric.setParentId(id);
                    Preconditions.checkArgument(StringUtils.isNotEmpty(selectedMetric.getAggregator()),
                            "SelectedMetric aggregator must not be empty");
                    if (!isMulti && selectedMetric.isMultiTimeseries()) {
                        isMulti = true;
                    }
                    if (lastTagVFilters == null) {
                        lastTagVFilters = FilterUtils.getFilters(selectedMetric.getFilterMap());
                    } else {
                        String curTagVFilters = FilterUtils.getFilters(selectedMetric.getFilterMap());
                        if (lastTagVFilters.equals(curTagVFilters)) {
                            isSame = true;
                        } else {
                            isSame = false;
                        }
                        lastTagVFilters = curTagVFilters;
                    }
                }

                Preconditions.checkArgument(!isMulti || (isMulti && isSame),
                        "metrics tags must agg to one or agg to multi and tags are same");
                if (isMulti) {
                    type = 2; //TODO not easy to use
                    metricCompose.setInnerJoin();
                } else {
                    type = 3;
                    metricCompose.setCrossJoin();
                }
            }
        }

        Preconditions.checkArgument(durationCondition != null ||
                        noDataCondition != null || eventCondition != null || params != null,
                "condition or no-data or event condition must not be null");
        if (durationCondition != null) {
            durationCondition.validate();
        }
        if (noDataCondition != null) {
            noDataCondition.validate();
        }
        if (eventCondition != null) {
            eventCondition.validate();
        }

    }

    private void monitorValidate() throws Exception {

    }

    public void validate() throws Exception {
        Preconditions.checkArgument(StringUtils.isNotEmpty(group), "group must not be empty");
        Preconditions.checkArgument(StringUtils.isNotEmpty(name), "name must not be empty");
        Preconditions.checkArgument(StringUtils.isNotEmpty(appName), "appName must not be empty");
        if (metric != null || metricCompose != null) {
            metricValidate();
        }

        if (monitorCompose != null) {
            monitorValidate();
        }
    }

    @JsonIgnore
    public String getDurationConditionId() {
        Preconditions.checkArgument(durationCondition != null, "condition is null");
        if (durationConditionId == null) {
            durationConditionId = getAggregateId() + "-" + durationCondition.hashCode();
        }
        return durationConditionId;
    }

    @JsonIgnore
    public String getNoDataConditionId() {
        Preconditions.checkArgument(noDataCondition != null, "noDataCondition is null");
        if (noDataConditionId == null) {
            noDataConditionId =
                    id + "-" + metric + "-" + FilterUtils.getFilters(getFilterMap()) + "-" + noDataCondition.hashCode();
        }
        return noDataConditionId;
    }

    @JsonProperty("monitor_compose")
    public MonitorCompose getMonitorCompose() {
        return monitorCompose;
    }

    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    @JsonProperty("group")
    public String getGroup() {
        return group;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("app_name")
    public String getAppName() {
        return appName;
    }

    @JsonProperty("detect_type")
    public String getDetectType() {
        return detectType;
    }

    @JsonProperty("condition")
    public DurationCondition getDurationCondition() {
        return durationCondition;
    }

    @JsonProperty("no_data_condition")
    public NoDataCondition getNoDataCondition() {
        return noDataCondition;
    }

    @JsonProperty("event_condition")
    public EventCondition getEventCondition() {
        return eventCondition;
    }

    @JsonIgnore
    public int getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Rule rule = (Rule)o;

        if (rate != rule.rate) {
            return false;
        }
        if (!Objects.equals(id, rule.id)) {
            return false;
        }
        if (!Objects.equals(group, rule.group)) {
            return false;
        }
        if (!Objects.equals(name, rule.name)) {
            return false;
        }
        if (!Objects.equals(appName, rule.appName)) {
            return false;
        }
        if (!Objects.equals(metric, rule.metric)) {
            return false;
        }
        if (!Objects.equals(tags, rule.tags)) {
            return false;
        }
        if (!Objects.equals(aggregator, rule.aggregator)) {
            return false;
        }
        if (!Objects.equals(durationCondition, rule.durationCondition)) {
            return false;
        }
        if (!Objects.equals(noDataCondition, rule.noDataCondition)) {
            return false;
        }
        return Objects.equals(eventCondition, rule.eventCondition);
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (group != null ? group.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (appName != null ? appName.hashCode() : 0);
        result = 31 * result + (metric != null ? metric.hashCode() : 0);
        result = 31 * result + (tags != null ? tags.hashCode() : 0);
        result = 31 * result + (rate ? 1 : 0);
        result = 31 * result + (aggregator != null ? aggregator.hashCode() : 0);
        result = 31 * result + (durationCondition != null ? durationCondition.hashCode() : 0);
        result = 31 * result + (noDataCondition != null ? noDataCondition.hashCode() : 0);
        result = 31 * result + (eventCondition != null ? eventCondition.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Rule{" + "id=" + id + ", group='" + group + '\'' + ", name='" + name + '\'' + ", appName='" + appName
                + '\'' + ", metric='" + metric + '\'' + ", filters={" + FilterUtils.getFilters(getFilterMap()) + "}, rate="
                + rate + ", aggregator='" + aggregator + '\'' + ", condition=" + durationCondition + ", noDataCondition="
                + noDataCondition + ", eventCondition=" + eventCondition + '}';
    }

    @JsonIgnore
    public String getComposeMetricName() {
        return getName() + "." + getId();
    }

    public boolean match(String name) {
        if (type == 0 || type == 1) {
            return name.equals(metric) || name.startsWith(prefix) && name.endsWith(suffix);
        } else {
            return name.equals(getComposeMetricName());
        }
    }

    @JsonProperty("metric_compose")
    public MetricCompose getMetricCompose() {
        return metricCompose;
    }

    @Override
    public String getQueryId() {
        return String.valueOf(id);
    }

    @Override
    public Long getParentId() {
        return id;
    }

    private void appendTags(Map<String ,String > tags, Map<String ,String > tags_sub, String id){
        for(String tag_sub_key: tags_sub.keySet()){
            tags.put(id+ "." + tag_sub_key, tags_sub.get(tag_sub_key));
        }
    }

    public DataPoint evaluate(List<DataPoint> dps) {
        if (dps.size() < 1) {
            LOG.warn("not enough dp for evaluate.");
            return null;
        }
        Map<String, String> tags = new HashMap<>();
        JexlExpression exp = ExpressionFactory.getExpression(metricCompose.getExpression());
        JexlContext expContext = new MapContext();
        for (DataPoint dp : dps) {
            int tagSize = 0;
            if (dp.getTags() != null) {
                tagSize = dp.getTags().size();
            }
            for (SelectedMetric metric : metricCompose.getMetrics()) {
                int filterSize = 0;
                if (metric.getFilterMap() != null) {
                    filterSize = metric.getFilterMap().size();
                }
                if (dp.getName().equals(metric.getMetric()) && FilterUtils.matchTags(dp.getTags(),
                        metric.getFilterMap())
                        && tagSize == filterSize) {
                    expContext.set(metric.getId(), dp.getValue());
                    if (type == 3) {
                        appendTags(tags, dp.getTags(), metric.getId());
                    } else {
                        tags.putAll(dp.getTags());
                    }

                }
            }
        }

        try {
            Object eo = exp.evaluate(expContext);

            double value = 0;
            if (eo instanceof Double || eo instanceof Long || eo instanceof Integer || eo instanceof Float) {
                value = Double.parseDouble(eo.toString());
            } else if (eo instanceof Boolean) {
                value = ((Boolean) eo) ? 1.0d : 0.0d;
            } else {
                LOG.info(eo.getClass().getName() + " no support type");
            }

            DataPoint dataPoint = new ImmutableDataPoint(getComposeMetricName(), dps.get(0).getTimestamp(), value,
                    tags);

            return dataPoint;
        } catch (Exception e) {
            LOG.error("evaluate error." + e.getMessage() + " " + this.toString());
        }

        return null;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setDurationConditionId(String durationConditionId) {
        this.durationConditionId = durationConditionId;
    }

    public void setNoDataConditionId(String noDataConditionId) {
        this.noDataConditionId = noDataConditionId;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public void setMetricCompose(MetricCompose metricCompose) {
        this.metricCompose = metricCompose;
    }

    public void setMonitorCompose(MonitorCompose monitorCompose) {
        this.monitorCompose = monitorCompose;
    }

    public void setCompose(boolean compose) {
        this.compose = compose;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public void setDurationCondition(DurationCondition durationCondition) {
        this.durationCondition = durationCondition;
    }

    public void setNoDataCondition(NoDataCondition noDataCondition) {
        this.noDataCondition = noDataCondition;
    }

    public void setEventCondition(EventCondition eventCondition) {
        this.eventCondition = eventCondition;
    }

    public void setDetectType(String detectType) {
        this.detectType = detectType;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }
}

