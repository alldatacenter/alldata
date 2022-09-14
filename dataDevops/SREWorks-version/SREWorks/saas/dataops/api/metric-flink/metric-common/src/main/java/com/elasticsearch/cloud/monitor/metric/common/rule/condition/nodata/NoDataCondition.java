package com.elasticsearch.cloud.monitor.metric.common.rule.condition.nodata;

import com.elasticsearch.cloud.monitor.metric.common.checker.nodata.NoDataConditionChecker;
import com.elasticsearch.cloud.monitor.metric.common.core.AlarmLevel;
import com.elasticsearch.cloud.monitor.metric.common.core.Constants;
import com.elasticsearch.cloud.monitor.metric.common.rule.condition.Condition;
import com.elasticsearch.cloud.monitor.metric.common.rule.filter.TagVFilter;
import com.elasticsearch.cloud.monitor.metric.common.utils.TimeUtils;
import com.fasterxml.jackson.annotation.*;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import org.apache.commons.collections.MapUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {@JsonSubTypes.Type(name = "no_data", value = NoDataCondition.class),})
public class NoDataCondition implements Condition {

    private Map<AlarmLevel, String> thresholds;
    private boolean isSeriesGroupBy;
    private volatile List<Map<String,String>> noDataLines;

    @JsonIgnore
    private Map<AlarmLevel, Double> numberThresholds;

    public NoDataCondition(){}

    @JsonCreator
    public NoDataCondition(@JsonProperty("thresholds") Map<AlarmLevel, String> thresholds) {
        this.thresholds = thresholds;
    }

    @Override
    public void validate() throws Exception {
        Preconditions.checkArgument(MapUtils.isNotEmpty(thresholds), "thresholds must not be empty");
        this.numberThresholds = new HashMap<>();
        for (Map.Entry<AlarmLevel, String> entry : thresholds.entrySet()) {
            String value = entry.getValue();
            long time = TimeUtils.parseDuration(value);
            Preconditions.checkArgument(time >= Constants.CHECK_INTERVAL,
                "threshold value(" + value + ") must be >= " + Constants.CHECK_INTERVAL_STR);
            Preconditions.checkArgument(time % Constants.CHECK_INTERVAL == 0,
                "threshold value(" + value + ") must be a multiple of " + Constants.CHECK_INTERVAL_STR);

            // limit  upgrade to 24h
            Preconditions.checkArgument(time <= 24* Constants.MAX_DURATION_TIME,
                "threshold value(" + value + ") must be less than  24*" + Constants.MAX_DURATION_TIME);
            numberThresholds.put(entry.getKey(), ((double) time / Constants.CHECK_INTERVAL));
        }
    }

    public AlarmLevel check(double value) {
        Preconditions.checkArgument(numberThresholds != null, "numberThresholds is null, validate method not called");
        for (AlarmLevel level : AlarmLevel.values()) {
            if (!numberThresholds.containsKey(level)) {
                continue;
            }
            double threshold = numberThresholds.get(level);
            if (value >= threshold) {
                return level;
            }
        }
        return null;
    }

    @JsonIgnore
    public NoDataConditionChecker getChecker() {
        return getChecker(null);
    }

    @JsonIgnore
    public NoDataConditionChecker getChecker(Map<String, TagVFilter> tagVFilterMap) {
        if (getIsSeriesGroupBy()) {
            return new NoDataConditionChecker(this, tagVFilterMap);
        } else {
            return new NoDataConditionChecker(this, null);
        }
    }

    @JsonProperty("thresholds")
    public Map<AlarmLevel, String> getThresholds() {
        return thresholds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return java.util.Objects.equals(thresholds, ((NoDataCondition) o).thresholds);
    }

    @Override
    public int hashCode() {
        int h = 0;
        for (Map.Entry<AlarmLevel, String> entry : thresholds.entrySet()) {
            h += Objects.hashCode(entry.getKey().toString()) ^ Objects.hashCode(entry.getValue());
        }
        return h;
    }

    @Override
    public String toString() {
        return "NoDataCondition{" + "thresholds=" + getThresholds() + '}';
    }

    public void setThresholds(Map<AlarmLevel, String> thresholds) {
        this.thresholds = thresholds;
    }

    public Map<AlarmLevel, Double> getNumberThresholds() {
        return numberThresholds;
    }

    public void setNumberThresholds(Map<AlarmLevel, Double> numberThresholds) {
        this.numberThresholds = numberThresholds;
    }

    public boolean getIsSeriesGroupBy() {
        return isSeriesGroupBy;
    }

    public void setIsSeriesGroupBy(boolean seriesGroupBy) {
        isSeriesGroupBy = seriesGroupBy;
    }

    public List<Map<String, String>> getNoDataLines() {
        return noDataLines;
    }

    public void setNoDataLines(List<Map<String, String>> noDataLines) {
        this.noDataLines = noDataLines;
    }

    public Set<String> getNoDataLineTagKeys() {
        List<Map<String, String>> tempNoDataLines = noDataLines;
        if (tempNoDataLines == null || tempNoDataLines.size() == 0) {
            return null;
        } else {
            return tempNoDataLines.get(0).keySet();
        }
    }
}
