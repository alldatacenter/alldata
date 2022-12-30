package com.elasticsearch.cloud.monitor.metric.common.rule.failure;

import com.alibaba.fastjson.JSONObject;
import com.elasticsearch.cloud.monitor.metric.common.utils.TimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * 故障规则
 *
 * @author: fangzong.lyj
 * @date: 2022/01/27 15:47
 */
public class FailureRule {
    private List<FailureLevelWithBoundary> failureLevels = new ArrayList<>();

    public void checkAndLoad(JSONObject rawRules) {
        Long upperDuration = null;
        if (rawRules.containsKey(FailureLevel.P0.name())) {
            FailureLevelWithBoundary p0 = new FailureLevelWithBoundary(FailureLevel.P0);
            p0.setLowerBoundary(TimeUtils.parseDuration(rawRules.getString(FailureLevel.P0.name())));
            upperDuration = p0.getLowerBoundary();
            failureLevels.add(p0);
        }

        if (rawRules.containsKey(FailureLevel.P1.name())) {
            FailureLevelWithBoundary p1 = new FailureLevelWithBoundary(FailureLevel.P1);
            p1.setLowerBoundary(TimeUtils.parseDuration(rawRules.getString(FailureLevel.P1.name())));
            p1.setUpperBoundary(upperDuration);
            upperDuration = p1.getLowerBoundary();
            failureLevels.add(p1);
        }

        if (rawRules.containsKey(FailureLevel.P2.name())) {
            FailureLevelWithBoundary p2 = new FailureLevelWithBoundary(FailureLevel.P2);
            p2.setLowerBoundary(TimeUtils.parseDuration(rawRules.getString(FailureLevel.P2.name())));
            p2.setUpperBoundary(upperDuration);
            upperDuration = p2.getLowerBoundary();
            failureLevels.add(p2);
        }

        if (rawRules.containsKey(FailureLevel.P3.name())) {
            FailureLevelWithBoundary p3 = new FailureLevelWithBoundary(FailureLevel.P3);
            p3.setLowerBoundary(TimeUtils.parseDuration(rawRules.getString(FailureLevel.P3.name())));
            p3.setUpperBoundary(upperDuration);
            upperDuration = p3.getLowerBoundary();
            failureLevels.add(p3);
        }

        if (rawRules.containsKey(FailureLevel.P4.name())) {
            FailureLevelWithBoundary p4 = new FailureLevelWithBoundary(FailureLevel.P4);
            p4.setLowerBoundary(TimeUtils.parseDuration(rawRules.getString(FailureLevel.P4.name())));
            p4.setUpperBoundary(upperDuration);
            failureLevels.add(p4);
        }
    }

    public FailureLevelWithBoundary getFailureLevel(Long durationTs) {
        Optional<FailureLevelWithBoundary> first = failureLevels.stream().filter(failureLevel -> failureLevel.isInBoundary(durationTs, BoundaryMode.LOWER_CLOSE_UPPER_OPEN)).findFirst();
        return first.orElse(null);
    }
}
