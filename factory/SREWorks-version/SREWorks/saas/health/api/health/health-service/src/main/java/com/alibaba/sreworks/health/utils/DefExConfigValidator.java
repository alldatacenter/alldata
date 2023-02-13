package com.alibaba.sreworks.health.utils;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.health.common.constant.Constant;
import com.alibaba.sreworks.health.domain.bo.*;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;


/**
 * 扩展配置合法性校验器
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/05 10:16
 */
public class DefExConfigValidator {
    public static void validateAlertExConfig(AlertExConfig alertExConfig) {
        AlertRuleConfig alertRuleConfig = alertExConfig.getAlertRuleConfig();
        Preconditions.checkNotNull(alertExConfig.getMetricId(), "提供阈值规则时,必须关联指标");

        Preconditions.checkArgument(alertRuleConfig.getTimes() > 0, String.format("持续次数需要大于0, 当前取值:%s", alertRuleConfig.getTimes()));
        Preconditions.checkArgument(alertRuleConfig.getDuration() >= 0, String.format("持续时间需要非负, 当前取值:%s", alertRuleConfig.getDuration()));
        Preconditions.checkArgument(Constant.ALERT_RULE_OPERATORS.contains(alertRuleConfig.getComparator()),
                String.format("比较操作符非法,合法取值:%s 当前取值:%s", Constant.ALERT_RULE_OPERATORS, alertRuleConfig.getComparator()));
        Preconditions.checkArgument(alertExConfig.getWeight() <= 10 && alertExConfig.getWeight() >= 0, String.format("权重值合理范围[0,10], 当前取值:%s", alertExConfig.getWeight()));
    }

    public static void validateFailureExConfig(FailureExConfig failureExConfig) {
        Preconditions.checkArgument(failureExConfig.getRefIncidentDefId() != null, "故障定义失败, 需要明确指定关联的异常定义");
        Preconditions.checkArgument(failureExConfig.getFailureLevelRule() != null && !failureExConfig.getFailureLevelRule().isEmpty(), "故障定义失败, 需要明确指定故障定级规则");

        JSONObject failureLevelRule = failureExConfig.getFailureLevelRule();
        for (String level : failureLevelRule.keySet()) {
            Preconditions.checkArgument(Constant.FAILURE_LEVEL_PATTERN.matcher(level).find(), "故障等级参数非法,目前仅支持[P4, P3, P2, P1, P0]");
        }
    }

    public static void validateRiskExConfig(RiskExConfig riskExConfig) {
        Preconditions.checkArgument(riskExConfig.getWeight() <= 10 && riskExConfig.getWeight() >= 0, String.format("权重值合理范围[0,10], 当前取值:%s", riskExConfig.getWeight()));
    }

    public static void validateIncidentExConfig(IncidentExConfig incidentExConfig) {
        Preconditions.checkArgument(incidentExConfig.getWeight() <= 10 && incidentExConfig.getWeight() >= 0, String.format("权重值合理范围[0,10], 当前取值:%s", incidentExConfig.getWeight()));
    }


    public static void validateEventExConfig(EventExConfig eventExConfig) {
        Preconditions.checkArgument(StringUtils.isNotEmpty(eventExConfig.getType()), "事件定义失败, 事件类型不能为空");
    }
}
