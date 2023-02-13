package com.elasticsearch.cloud.monitor.metric.common.rule.failure;

import com.elasticsearch.cloud.monitor.metric.common.blink.utils.FlinkTimeUtil;

/**
 * 故障规则上下界
 *
 * @author: fangzong.lyj
 * @date: 2022/01/27 14:38
 */
public class FailureLevelWithBoundary {

    private FailureLevel level;

    private Long lowerBoundary;

    private Long upperBoundary;

    public FailureLevelWithBoundary(FailureLevel level) {
        this(level, null, null);
    }

    public FailureLevelWithBoundary(FailureLevel level, Long lowerBoundary, Long upperBoundary) {
        this.level = level;
        this.lowerBoundary = lowerBoundary == null ? Long.MIN_VALUE : lowerBoundary;
        this.upperBoundary = upperBoundary == null ? Long.MAX_VALUE : upperBoundary;
    }

    public void setLevel(FailureLevel level) {
        this.level = level;
    }

    public void setLowerBoundary(Long lowerBoundary) {
        this.lowerBoundary = lowerBoundary;
    }

    public void setUpperBoundary(Long upperBoundary) {
        this.upperBoundary = upperBoundary;
    }

    public FailureLevel getLevel() {
        return level;
    }

    public Long getLowerBoundary() {
        return lowerBoundary;
    }

    public Long getUpperBoundary() {
        return upperBoundary;
    }

    public boolean isInBoundary(Long ts, BoundaryMode mode) {
        if (mode == BoundaryMode.LOWER_CLOSE_UPPER_OPEN) {
            return ts >= lowerBoundary && ts < upperBoundary;
        }

        if (mode == BoundaryMode.LOWER_OPEN_UPPER_CLOSE) {
            return ts > lowerBoundary && ts <= upperBoundary;
        }

        return ts >= lowerBoundary && ts <= upperBoundary;
    }

    @Override
    public String toString() {
        if (lowerBoundary == null || lowerBoundary == Long.MIN_VALUE) {
            return "故障规则{" +
                    "故障等级=" + level +
                    ", 阈值上限=" + FlinkTimeUtil.getDuration(upperBoundary) +
                    '}';
        }

        if (upperBoundary == null || upperBoundary == Long.MAX_VALUE) {
            return "故障规则{" +
                    "故障等级=" + level +
                    ", 阈值下限=" + FlinkTimeUtil.getDuration(lowerBoundary) +
                    '}';
        }
        return "故障规则{" +
                "故障等级=" + level +
                ", 阈值下限=" + FlinkTimeUtil.getDuration(lowerBoundary) +
                ", 阈值上限=" + FlinkTimeUtil.getDuration(upperBoundary) +
                '}';
    }
}
