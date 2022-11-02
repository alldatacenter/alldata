package com.elasticsearch.cloud.monitor.metric.common.rule.loader;

import com.elasticsearch.cloud.monitor.metric.common.rule.Rule;
import com.elasticsearch.cloud.monitor.metric.common.rule.SubQuery;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * @author: fangzong.lyj
 * @date: 2021/09/02 14:20
 */
@Slf4j
public abstract class AbstractRulesLoader implements RulesLoader {
    private String lastVersion = null;

    public AbstractRulesLoader() {
    }

    public void setLastVersion(String version) {
        lastVersion = version;
    }

    public String getLastVersion() {
        return lastVersion;
    }

    protected abstract String readObject(String var1);

    protected void ruleToLowerCase(Rule rule) {
        if (rule != null) {
            this.subqueryLowerCase(rule);
            if (rule.getMetricCompose() != null && rule.getMetricCompose().getMetrics() != null) {
                rule.getMetricCompose().getMetrics().forEach(this::subqueryLowerCase);
            }
        }
    }

    protected void subqueryLowerCase(SubQuery rule) {
        if (rule != null) {
            if (StringUtils.isNotEmpty(rule.getMetric())) {
                rule.setMetric(rule.getMetric().toLowerCase());
            }

            if (rule.getTags() != null) {
                Map<String, String> newTag = Maps.newHashMap();
                rule.getTags().forEach((k, v) -> newTag.put(k, v == null ? null : v.toLowerCase()));
                rule.setTags(newTag);
            }

        }
    }
}