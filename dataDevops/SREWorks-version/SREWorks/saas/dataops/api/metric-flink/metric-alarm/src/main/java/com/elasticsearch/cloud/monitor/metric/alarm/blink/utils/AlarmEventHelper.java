package com.elasticsearch.cloud.monitor.metric.alarm.blink.utils;

import com.elasticsearch.cloud.monitor.metric.common.constant.Constants;
import com.elasticsearch.cloud.monitor.metric.common.core.MetricAlarm;
import com.elasticsearch.cloud.monitor.metric.common.rule.Rule;
import com.elasticsearch.cloud.monitor.metric.common.utils.TagUtils;
import com.elasticsearch.cloud.monitor.metric.common.utils.TimeUtils;
import com.google.common.hash.Hashing;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class AlarmEventHelper {

    /**
     * @param rule
     * @param metricAlarm
     * @param source      标识来自于那个blink job
     * @return
     */
    public static AlarmEvent buildEvent(Rule rule, MetricAlarm metricAlarm, String source) {
        AlarmEvent event = new AlarmEvent();
        event.setType(StringUtils.lowerCase(metricAlarm.getAlarm().getLevel().name()));
        event.setSource(source);

        event.setTitle(rule.getName());

        StringBuilder sb = new StringBuilder();
        sb.append("(");
        if (rule.isRate()) {
            sb.append("rate:");
        }
        if (StringUtils.isNotEmpty(rule.getAggregator())) {
            sb.append(rule.getAggregator()).append(":");
        }
        if (rule.isCompose()) {
            sb.append(rule.getComposeMetricName());
        } else {
            if (rule.getMetric() != null && rule.getMetric().contains("*")) {
                sb.append(metricAlarm.getMetric());
            } else {
                sb.append(rule.getMetric());
            }
        }
        Map<String, String> tags = new HashMap<>();
        if (metricAlarm.getTags() != null) {
            tags.putAll(metricAlarm.getTags());
        }
        tags.remove(Constants.TENANT_TAG);
        if (MapUtils.isNotEmpty(tags)) {
            sb.append("{").append(TagUtils.getTag(tags)).append("}");
        }
        sb.append(")");
        sb.append(metricAlarm.getAlarm().getMsg());

        event.setText(sb.toString());
        event.setTime(TimeUtils.toMillisecond(metricAlarm.getTimestamp()));
        event.setId(getMD5(rule.getId().toString() + metricAlarm.getTimestamp()));
        event.addTags(tags);
        event.addTag("__ruleid=" + rule.getId().toString());

        String[] strs = rule.getAppName().split(",");
        event.setService(strs[0]);
        event.setGroup(rule.getGroup());
        event.setUid(rule.getUid());

        if (metricAlarm.isError()) {
            event.addTag("__error=1");
        }
        return event;
    }

    private static String getMD5(String src) {
        return Hashing.md5().hashString(src, Charset.forName("utf8")).toString();
    }

}
