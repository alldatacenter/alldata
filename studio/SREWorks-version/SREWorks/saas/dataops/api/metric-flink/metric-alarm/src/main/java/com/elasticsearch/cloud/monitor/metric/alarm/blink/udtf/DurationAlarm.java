package com.elasticsearch.cloud.monitor.metric.alarm.blink.udtf;

import com.elasticsearch.cloud.monitor.metric.alarm.blink.constant.AlarmConstants;
import com.elasticsearch.cloud.monitor.metric.alarm.blink.utils.AlarmEvent;
import com.elasticsearch.cloud.monitor.metric.alarm.blink.utils.AlarmEventHelper;
import com.elasticsearch.cloud.monitor.metric.alarm.blink.utils.TagsUtils;
import com.elasticsearch.cloud.monitor.metric.common.cache.RuleConditionCache;
import com.elasticsearch.cloud.monitor.metric.common.cache.RuleConditionKafkaCache;
import com.elasticsearch.cloud.monitor.metric.common.blink.utils.FlinkLogTracer;
import com.elasticsearch.cloud.monitor.metric.common.checker.duration.DurationConditionChecker;
import com.elasticsearch.cloud.monitor.metric.common.client.KafkaConfig;
import com.elasticsearch.cloud.monitor.metric.common.core.Alarm;
import com.elasticsearch.cloud.monitor.metric.common.core.Constants;
import com.elasticsearch.cloud.monitor.metric.common.core.MetricAlarm;
import com.elasticsearch.cloud.monitor.metric.common.datapoint.DataPoint;
import com.elasticsearch.cloud.monitor.metric.common.datapoint.ImmutableDataPoint;
import com.elasticsearch.cloud.monitor.metric.common.rule.MinioRulesManager;
import com.elasticsearch.cloud.monitor.metric.common.rule.Rule;
import com.elasticsearch.cloud.monitor.metric.common.rule.SreworksRulesManagerFactory;
import com.elasticsearch.cloud.monitor.metric.common.rule.util.RuleUtil;
import com.elasticsearch.cloud.monitor.metric.common.utils.TagUtils;
import com.elasticsearch.cloud.monitor.metric.common.utils.TimeUtils;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.java.tuple.Tuple9;
import org.apache.flink.table.functions.FunctionContext;
import org.apache.flink.table.functions.TableFunction;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 特别提示: 同一条线, 必须放到同一个shard上且是保序的, 如果分布到多个shard上会有问题!!!
 *
 * @author xingming.xuxm
 * @Date 2019-12-11
 */

@SuppressWarnings("Duplicates")
@Slf4j
public class DurationAlarm
    extends TableFunction<Tuple9<String, String, String, String, String, String, Long, String, String>> {
    private transient SreworksRulesManagerFactory ruleManagerFactory;
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

    @Override
    public void open(FunctionContext context) throws Exception {
        ruleManagerFactory = RuleUtil.createRuleManagerFactoryForFlink(context);
        long timeout = Long.parseLong(context.getJobParameter(AlarmConstants.CHECKER_CACHE_TIMEOUT_HOUR, "1"));
        ruleConditionCaches = CacheBuilder.newBuilder().expireAfterAccess(timeout, TimeUnit.HOURS).build();

        cacheDelayMs = Long.parseLong(context.getJobParameter(AlarmConstants.CACHE_DELAY_MS, "600000"));

        if (enableRecoverCache) {
            // TODO Kafka恢复数据
            kafkaConfig = new KafkaConfig();
        }

        tracer = new FlinkLogTracer(context);
    }

    public void eval(Long ruleId, String metricName, Long timestamp, Double metricValue, String tagsStr, String granularity) {
        MinioRulesManager rulesManager = (MinioRulesManager)ruleManagerFactory.getRulesManager();
        if (rulesManager == null) {
            return;
        }

        Rule rule = rulesManager.getRule(ruleId);
        if (rule == null) {
            return;
        }

        Map<String, String> tags = TagsUtils.toTagsMap(tagsStr);
        long interval = TimeUtils.parseDuration(granularity);

        //这个如果并发情况下 可能会有问题 TODO
        Constants.CHECK_INTERVAL = interval;

        DataPoint dp = new ImmutableDataPoint(metricName, timestamp, metricValue, tags, granularity);
        RuleConditionCache ruleConditionCache = getRuleConditionCache(rule, interval, dp);
        if (ruleConditionCache == null) {
            log.error(String.format("ruleId %s getRuleConditionCache is null", ruleId));
            return;
        }
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
                collect(
                        Tuple9.of(event.getService(), event.getSource(), StringUtils.join(event.getTags(), ","),
                        event.getText(), event.getTitle(), event.getType(), event.getTime(), event.getGroup(),
                        event.getUid())
                );
            }
        } catch (Exception e) {
            log.error("check failed. ruleid=" + rule.getId() + " " + dp.getTimestamp() + TagUtils.getTag(dp.getTags()),
                e);
        }

        ruleConditionCache.compact(dp.getTimestamp());
    }

    /**
     * 获取当前rule规则 缓存的数据流历史数据，不同的流通过数据流tags区分
     *
     * @param rule 数据关联规则
     * @param interval 流入的数据粒度
     * @param dataPoint 流入的数据点
     * @return
     */
    private RuleConditionCache getRuleConditionCache(Rule rule, long interval, DataPoint dataPoint) {
        String key = getCacheKey(rule);
        RuleConditionCache ruleConditionCache = ruleConditionCaches.getIfPresent(key);
        if (ruleConditionCache == null) {
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
     * rule维度的cache, ruleid, tags, 判断条件, 变化都不再是同一个rule
     * 这里没有考虑metric name, 因为metric name 变化了, 基本上tags肯定会变
     *
     * @param rule
     * @return
     */
    private String getCacheKey(Rule rule) {
        return String.format("%d-%d-%s-cached", rule.getId(), rule.getFilterMap().hashCode(),
            rule.getDurationConditionId());
    }

    @Override
    public void close() throws Exception {
        tracer.trace("DurationAlarm close========");

        if (ruleManagerFactory != null) {
            ruleManagerFactory.close();
        }
    }
}
