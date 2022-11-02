package com.elasticsearch.cloud.monitor.metric.alarm.blink.udtf;

import com.alibaba.fastjson.JSONObject;
import com.elasticsearch.cloud.monitor.metric.alarm.blink.constant.AlarmConstants;
import com.elasticsearch.cloud.monitor.metric.alarm.blink.utils.AlarmEvent;
import com.elasticsearch.cloud.monitor.metric.alarm.blink.utils.AlarmEventHelper;
import com.elasticsearch.cloud.monitor.metric.common.cache.RuleConditionCache;
import com.elasticsearch.cloud.monitor.metric.common.cache.RuleConditionKafkaCache;
import com.elasticsearch.cloud.monitor.metric.common.blink.utils.FlinkLogTracer;
import com.elasticsearch.cloud.monitor.metric.common.blink.utils.FlinkTimeUtil;
import com.elasticsearch.cloud.monitor.metric.common.checker.duration.DurationConditionChecker;
import com.elasticsearch.cloud.monitor.metric.common.client.KafkaConfig;
import com.elasticsearch.cloud.monitor.metric.common.constant.Constants;
import com.elasticsearch.cloud.monitor.metric.common.core.Alarm;
import com.elasticsearch.cloud.monitor.metric.common.core.MetricAlarm;
import com.elasticsearch.cloud.monitor.metric.common.datapoint.DataPoint;
import com.elasticsearch.cloud.monitor.metric.common.datapoint.ImmutableDataPoint;
import com.elasticsearch.cloud.monitor.metric.common.pojo.AlertInstance;
import com.elasticsearch.cloud.monitor.metric.common.rule.ClientRulesManager;
import com.elasticsearch.cloud.monitor.metric.common.rule.HealthAlertRulesManagerFactory;
import com.elasticsearch.cloud.monitor.metric.common.rule.Rule;
import com.elasticsearch.cloud.monitor.metric.common.utils.HttpClientsUtil;
import com.elasticsearch.cloud.monitor.metric.common.utils.PropertiesUtil;
import com.elasticsearch.cloud.monitor.metric.common.utils.TagUtils;
import com.elasticsearch.cloud.monitor.metric.common.utils.TimeUtils;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.java.tuple.Tuple13;
import org.apache.flink.table.functions.FunctionContext;
import org.apache.flink.table.functions.TableFunction;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 特别提示: 同一条线, 必须放到同一个shard上且是保序的, 如果分布到多个shard上会有问题!!!
 *
 * @author: fangzong.lyj
 * @date: 2021/09/01 15:40
 */

@SuppressWarnings("Duplicates")
@Slf4j
public class HealthAlert
    extends TableFunction<Tuple13<Integer, Integer, String, String, String, String, String, String, String, String, String, Long, String>> {
    private transient HealthAlertRulesManagerFactory rulesManagerFactory;
    private transient Cache<String, RuleConditionCache> ruleConditionCaches;

    /**
     * flink暂时不上报作业的监控指标, 需要提供新的监控数据上报通道
     */
    private FlinkLogTracer tracer;
    private KafkaConfig kafkaConfig = null;
    private boolean enableRecoverCache = false;

    /**
     * cache延迟10min, 否则追数据很慢
     */
    private long cacheDelayMs = 10 * 60 * 1000;

    private final int maxTotal = 200;
    private final int maxPerRoute = 40;
    private final int maxRoute = 100;
    private final String pushAlertInstancePath = "/alert_instance/pushAlerts";

    private CloseableHttpClient httpClient;

    @Override
    public void open(FunctionContext context) throws Exception {
        long refreshPeriod = Long.parseLong(context.getJobParameter(Constants.RULE_REFRESH_PERIOD, Constants.RULE_REFRESH_PERIOD_DEF + ""));
        long shufflePeriod = Long.parseLong(context.getJobParameter(Constants.RULE_REFRESH_SHUFFLE, Constants.RULE_REFRESH_SHUFFLE_DEF + ""));
        rulesManagerFactory = new HealthAlertRulesManagerFactory(refreshPeriod, shufflePeriod);

        httpClient = HttpClientsUtil.createHttpClient(maxTotal, maxPerRoute, maxRoute, PropertiesUtil.getProperty("health.instance.url"));

        long timeout = Long.parseLong(context.getJobParameter(AlarmConstants.CHECKER_CACHE_TIMEOUT_HOUR, "1"));
        ruleConditionCaches = CacheBuilder.newBuilder().expireAfterAccess(timeout, TimeUnit.HOURS).build();

        cacheDelayMs = Long.parseLong(context.getJobParameter(AlarmConstants.CACHE_DELAY_MS, "600000"));

        if (enableRecoverCache) {
            // TODO Kafka恢复数据
            kafkaConfig = new KafkaConfig();
        }

        tracer = new FlinkLogTracer(context);
    }

    public void eval(String metricInstanceUid, Integer metricId, String metricName, Map<String, String> labels, Long ts, Float value, Integer alertDefId, String appId, String alertConfigStr) {
        // alertConfig 带有告警规则配置和指标的数据粒度配置，需要丰富后传递给规则管理类，生成告警规则缓存数据
        JSONObject alertConfig = JSONObject.parseObject(alertConfigStr);
        if (CollectionUtils.isEmpty(alertConfig)) {
            throw new IllegalArgumentException("alert rule config is empty");
        }

        Long ruleId = generateRuleId(alertDefId, metricId);
        ClientRulesManager rulesManager = rulesManagerFactory.getRulesManager(String.valueOf(alertDefId));
        boolean ruleUpdated = false;
        if (rulesManager == null) {
            JSONObject alertRuleConfig = buildAlertRuleConfig(appId, metricName, ruleId, alertConfig.getJSONObject("alert_rule_config"));
            rulesManager = rulesManagerFactory.getRulesManager(String.valueOf(alertDefId), JSONObject.toJSONString(alertRuleConfig));
            if (rulesManager == null) {
                log.warn(String.format("build rules manager null[rules_manager_key:%s]", alertDefId));
                return;
            }
            ruleUpdated = rulesManager.isUpdated();
        }

        Rule rule = rulesManager.getRule(ruleId);
        if (rule == null) {
            log.warn(String.format("not exist rule [rule_id:%s]", ruleId));
            return;
        }

        String granularity = FlinkTimeUtil.getMinuteDuration(alertConfig.getInteger("granularity"));
        long interval = TimeUtils.parseDuration(granularity);
        long timestamp = ts - ts % interval;   // 时间戳对齐

        //这个如果并发情况下 可能会有问题 TODO
//        com.elasticsearch.cloud.monitor.commons.core.Constants.CHECK_INTERVAL = interval;

        DataPoint dp = new ImmutableDataPoint(metricName, timestamp, value, labels, granularity);
        RuleConditionCache ruleConditionCache = getRuleConditionCache(rule, ruleUpdated, metricInstanceUid, interval, dp);
        DurationConditionChecker checker = ruleConditionCache.getConditionChecker(dp.getTags());
        ruleConditionCache.put(dp);
        try {
            Alarm alarm = checker.check(dp, ruleConditionCache);
            if (alarm != null && !alarm.isOk()) {
                MetricAlarm metricAlarm = new MetricAlarm();
                metricAlarm.setAlarm(alarm);
                metricAlarm.setRuleId(rule.getId());
                metricAlarm.setTags(dp.getTags());
                metricAlarm.setTimestamp(TimeUtils.toMillisecond(dp.getTimestamp()));
                metricAlarm.setError(false);
                if (rule.getMetric() != null && rule.getMetric().contains("*")) {
                    metricAlarm.setMetric(dp.getName());
                }
                AlarmEvent event = AlarmEventHelper.buildEvent(rule, metricAlarm, this.getClass().getSimpleName());

                String appInstanceId = dp.getTags().getOrDefault("app_instance_id", "");
                String appComponentInstanceId = dp.getTags().getOrDefault("app_component_instance_id", "");

                AlertInstance alert = AlertInstance.builder()
                        .alertDefId(alertDefId)
                        .appInstanceId(appInstanceId)
                        .appComponentInstanceId(appComponentInstanceId)
                        .metricInstanceId(metricInstanceUid)
                        .metricInstanceLabels(JSONObject.parseObject(JSONObject.toJSONString(dp.getTags())))
                        .occurTs(ts)
                        .source(event.getSource())
                        .level(event.getType())
//                        .receivers()
                        .content(event.getText())
                        .build();
                // 推送告警实例
                try {
                    String response = HttpClientsUtil.post(httpClient,
                            PropertiesUtil.getProperty("health.instance.url") + pushAlertInstancePath + "?defId=" + alertDefId,
                            JSONObject.toJSONString(Collections.singleton(alert)));
                    JSONObject result = JSONObject.parseObject(response);
                    Integer code = result.getInteger("code");
                    if (code == null || !code.equals(200)) {
                        log.error("push alert instance failed", result);
                        throw new IllegalArgumentException(response);
                    }
                } catch (Throwable e) {
                    log.error("push alert instance failed", e);
                    throw new IllegalArgumentException(e);
                }

                collect(
                        Tuple13.of(alertDefId, metricId, metricName, appInstanceId, appComponentInstanceId,
                                metricInstanceUid, StringUtils.join(event.getTags(), ","), event.getGroup(),
                                event.getTitle(), event.getType(), event.getText(), ts, event.getSource())
                );
            }
        } catch (Exception e) {
            log.error("check failed. ruleid=" + rule.getId() + " " + dp.getTimestamp() + TagUtils.getTag(dp.getTags()),
                e);
        }

        ruleConditionCache.compact(dp.getTimestamp());
    }

    private JSONObject buildAlertRuleConfig(String appName, String metricName, Long ruleId, JSONObject rawCondition) {
        JSONObject alertConfig = new JSONObject();
        alertConfig.put("app_name", appName);
        alertConfig.put("metric", metricName);
        alertConfig.put("aggregator", "avg");
        alertConfig.put("name", metricName + "告警项监控");
        alertConfig.put("id", ruleId);
        alertConfig.put("group", "health-alert-metric");
        alertConfig.put("tags", new JSONObject());


        JSONObject valueThresholdCondition = new JSONObject();
        valueThresholdCondition.put("duration", FlinkTimeUtil.getMinuteDuration(rawCondition.getInteger("duration")));
        valueThresholdCondition.put("comparator", rawCondition.getString("comparator"));
        valueThresholdCondition.put("thresholds", JSONObject.parseObject(rawCondition.getJSONObject("thresholds").toJSONString().toUpperCase()));
//        valueThresholdCondition.put("thresholds", rawCondition.getJSONObject("thresholds"));
        valueThresholdCondition.put("type", "value_threshold");
        valueThresholdCondition.put("math_abs", rawCondition.getString("math_abs"));


        int count = rawCondition.getIntValue("times");
        if (count > 1) {
            JSONObject continuousCondition = new JSONObject();
            continuousCondition.put("condition", valueThresholdCondition);
            continuousCondition.put("count", count);
            continuousCondition.put("type", "continuous");
            alertConfig.put("condition", continuousCondition);
        } else {
            alertConfig.put("condition", valueThresholdCondition);
        }

        return alertConfig;
    }

    /**
     * 生成告警规则ID
     * 告警规则与告警定义强绑定,附带有一个告警定义关联的指标ID
     * @param alertDefId
     * @param metricId
     * @return
     */
    private Long generateRuleId(Integer alertDefId, Integer metricId) {
        return alertDefId * 10000000L + metricId;
    }

    /**
     * 获取当前rule规则 缓存的数据流历史数据，不同的流通过数据流tags区分
     *
     * @param rule 数据关联规则
     * @param interval 流入的数据粒度
     * @param dataPoint 流入的数据点
     * @return
     */
    private RuleConditionCache getRuleConditionCache(Rule rule, boolean ruleUpdated, String instanceId, long interval, DataPoint dataPoint) {
        String key = getCacheKey(rule.getId(), instanceId);
        RuleConditionCache ruleConditionCache = ruleConditionCaches.getIfPresent(key);
        if (ruleConditionCache == null || ruleUpdated) {
            log.warn(String.format("not exist rules condition cache[cache_key:%s] or rule updated[%s]", key, ruleUpdated));
            ruleConditionCache = new RuleConditionKafkaCache(rule, interval, kafkaConfig);
            ruleConditionCaches.put(key, ruleConditionCache);
        }
        try {
            // 规则需要的数据时间长度大于缓存数据时间长度, 通常第一次作业启动需要cache数据
            long crossSpan = rule.getDurationCondition().getCrossSpan() - interval;
            if (enableRecoverCache && crossSpan > cacheDelayMs) {
                ruleConditionCache.recovery(dataPoint);
            }
        } catch (Exception ex) {
            //可能是ruleConditionCache.recovery出现异常了
            log.error(String
                .format("fetch cache error. ruleid: %s,error %s ", rule.getId(), Throwables.getStackTraceAsString(ex)));
        }
        return ruleConditionCache;
    }

    /**
     * 生成缓存时间序列唯一Key(同一个告警定义或者指标下，会有多条曲线)
     * 根据告警规则ID(唯一确定告警定义/指标, 健康管理下同一个指标定义仅能关联一个告警定义) + 时间序列的labels(区分同一指标下的多条曲线)
     * @param ruleId
     * @param instanceId 指标实例ID(指标ID+时间序列的labels计算的唯一ID)
     * @return
     */
    private String getCacheKey(Long ruleId, String instanceId) {
        return String.format("%d-%s-cached", ruleId, instanceId);
    }

    @Override
    public void close() throws Exception {
        tracer.trace("DurationAlarm close========");

        if (rulesManagerFactory != null) {
            rulesManagerFactory.close();
        }
    }
}
