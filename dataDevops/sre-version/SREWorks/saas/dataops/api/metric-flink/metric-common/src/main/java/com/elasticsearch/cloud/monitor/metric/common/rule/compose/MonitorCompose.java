package com.elasticsearch.cloud.monitor.metric.common.rule.compose;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2022/06/11 13:49
 */
public class MonitorCompose {

    @JsonProperty("monitors")
    private List<Monitor> monitors;

    @JsonProperty("expression")
    private String expression;

    public List<Monitor> getMonitors() {
        return monitors;
    }

    public void setMonitors(List<Monitor> monitors) {
        this.monitors = monitors;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }
}
