package com.elasticsearch.cloud.monitor.metric.common.rule.condition;

import com.elasticsearch.cloud.monitor.metric.common.core.AlarmLevel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import org.apache.commons.collections.MapUtils;

import java.util.*;

public class ThresholdCondition implements Condition {
    private static final List<String> COMPARATORS = Arrays.asList(">", ">=", "<", "<=");
    private final boolean mathAbs;
    private final String comparator;
    private final Map<AlarmLevel, Double> thresholds;

    public ThresholdCondition(final boolean mathAbs, final String comparator,
        final Map<AlarmLevel, Double> thresholds) {
        this.mathAbs = mathAbs;
        this.comparator = comparator;
        this.thresholds = thresholds;
    }

    @Override
    public void validate() throws Exception {
        Preconditions.checkArgument(comparator != null, "comparator must not be null");
        Preconditions.checkArgument(MapUtils.isNotEmpty(thresholds),
            "thresholds must not be empty");
        Preconditions.checkArgument(COMPARATORS.contains(comparator), "illegal comparator");

        List<AlarmLevel> levels = new ArrayList<>();
        levels.addAll(thresholds.keySet());
        for(AlarmLevel level: levels){
            if(thresholds.get(level) == null){
                thresholds.remove(level);
            }
        }
    }

    public AlarmLevel getLevelOnValue(double value) {
        double val = this.mathAbs ? Math.abs(value) : value;
        for (AlarmLevel level : AlarmLevel.values()) {
            if (!thresholds.containsKey(level)) {
                continue;
            }
            double threshold = thresholds.get(level);
            if (">".equals(comparator) && val > threshold) {
                return level;
            } else if (">=".equals(comparator) && val >= threshold) {
                return level;
            } else if ("<".equals(comparator) && val < threshold) {
                return level;
            } else if ("<=".equals(comparator) && val <= threshold) {
                return level;
            }
        }
        return AlarmLevel.NORMAL;
    }

    @JsonProperty("math_abs")
    public boolean isMathAbs() {
        return mathAbs;
    }

    @JsonProperty("comparator")
    public String getComparator() {
        return comparator;
    }

    @JsonProperty("thresholds")
    public Map<AlarmLevel, Double> getThresholds() {
        return thresholds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ThresholdCondition that = (ThresholdCondition) o;

        if (mathAbs != that.mathAbs) return false;
        if (comparator != null ? !comparator.equals(that.comparator) : that.comparator != null)
            return false;
        return thresholds != null ? thresholds.equals(that.thresholds) : that.thresholds == null;
    }

    @Override
    public int hashCode() {
        int result = (mathAbs ? 1 : 0);
        result = 31 * result + (comparator != null ? comparator.hashCode() : 0);
        result = 31 * result + getThresholdsHashcode();
        return result;
    }

    // for changeless hashcode
    // enum's hashcode(the internal address of the object) is diff between jvm instances
    private int getThresholdsHashcode() {
        if (thresholds == null) {
            return 0;
        }
        int h = 0;
        for (Map.Entry<AlarmLevel, Double> entry : thresholds.entrySet()) {
            h += Objects.hashCode(entry.getKey().toString()) ^ Objects.hashCode(entry.getValue());
        }
        return h;
    }
}
