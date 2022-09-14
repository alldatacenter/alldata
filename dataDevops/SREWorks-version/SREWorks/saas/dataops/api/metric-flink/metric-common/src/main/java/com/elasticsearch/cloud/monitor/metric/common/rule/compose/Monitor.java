package com.elasticsearch.cloud.monitor.metric.common.rule.compose;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2022/06/11 13:47
 */
public class Monitor {
    @JsonProperty("id")
    private String id;

    @JsonIgnore
    public Integer parentId;

    @JsonProperty("rule_id")
    private Integer ruleId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public int getRuleId() {
        return ruleId;
    }

    public void setRuleId(Integer ruleId) {
        this.ruleId = ruleId;
    }
}
