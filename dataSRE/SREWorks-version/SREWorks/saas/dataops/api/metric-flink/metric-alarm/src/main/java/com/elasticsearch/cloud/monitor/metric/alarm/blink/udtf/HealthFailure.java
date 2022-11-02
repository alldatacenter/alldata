package com.elasticsearch.cloud.monitor.metric.alarm.blink.udtf;

import com.alibaba.fastjson.JSONObject;
import com.elasticsearch.cloud.monitor.metric.common.blink.utils.FlinkLogTracer;
import com.elasticsearch.cloud.monitor.metric.common.constant.Constants;
import com.elasticsearch.cloud.monitor.metric.common.pojo.FailureInstance;
import com.elasticsearch.cloud.monitor.metric.common.rule.HealthFailureRulesManager;
import com.elasticsearch.cloud.monitor.metric.common.rule.HealthFailureRulesManagerFactory;
import com.elasticsearch.cloud.monitor.metric.common.rule.failure.FailureLevelWithBoundary;
import com.elasticsearch.cloud.monitor.metric.common.utils.HttpClientsUtil;
import com.elasticsearch.cloud.monitor.metric.common.utils.PropertiesUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.java.tuple.Tuple9;
import org.apache.flink.table.functions.FunctionContext;
import org.apache.flink.table.functions.TableFunction;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.util.CollectionUtils;

import java.util.Date;


/**
 * 特别提示: 同一条线, 必须放到同一个shard上且是保序的, 如果分布到多个shard上会有问题!!!
 *
 * @author: fangzong.lyj
 * @date: 2021/09/01 15:40
 */

@SuppressWarnings("Duplicates")
@Slf4j
public class HealthFailure
        extends TableFunction<Tuple9<Integer, String, String, Long, String, String, Long, Long, String>> {
    private transient HealthFailureRulesManagerFactory rulesManagerFactory;

    /**
     * flink暂时不上报作业的监控指标, 需要提供新的监控数据上报通道
     */
    private FlinkLogTracer tracer;

    private final int maxTotal = 200;
    private final int maxPerRoute = 40;
    private final int maxRoute = 100;
    private final String pushFailureInstancePath = "/failure_instance/pushFailure";

    private CloseableHttpClient httpClient;

    @Override
    public void open(FunctionContext context) throws Exception {
        long refreshPeriod = Long.parseLong(context.getJobParameter(Constants.RULE_REFRESH_PERIOD, Constants.RULE_REFRESH_PERIOD_DEF + ""));
        long shufflePeriod = Long.parseLong(context.getJobParameter(Constants.RULE_REFRESH_SHUFFLE, Constants.RULE_REFRESH_SHUFFLE_DEF + ""));
        httpClient = HttpClientsUtil.createHttpClient(maxTotal, maxPerRoute, maxRoute, PropertiesUtil.getProperty("health.instance.url"));
        rulesManagerFactory = new HealthFailureRulesManagerFactory(refreshPeriod, shufflePeriod);
        tracer = new FlinkLogTracer(context);
    }

    public void eval(Long incidentInstanceId, String appInstanceId, String appComponentInstanceId, Long occurTs, Long recoverTs, String cause, Integer failureDefId, String appName, String failureConfigStr) {
        JSONObject failureConfig = JSONObject.parseObject(failureConfigStr);
        if (CollectionUtils.isEmpty(failureConfig)) {
            throw new IllegalArgumentException("failure rule config is empty");
        }
        JSONObject failureLevelRule = failureConfig.getJSONObject("failure_level_rule");
        if (CollectionUtils.isEmpty(failureLevelRule)) {
            throw new IllegalArgumentException("failure rule config is empty");
        }

        HealthFailureRulesManager rulesManager = rulesManagerFactory.getRulesManager(String.valueOf(failureDefId));
        if (rulesManager == null) {
            rulesManager = rulesManagerFactory.getRulesManager(String.valueOf(failureDefId), JSONObject.toJSONString(failureLevelRule));
            if (rulesManager == null) {
                throw new IllegalArgumentException(String.format("failure rule manager is empty, failure_def_id:%d", failureDefId));
            }
        }

        Long duration = 0L;
        if (recoverTs < occurTs) {
            Date now = new Date();
            duration = now.getTime() - occurTs;
            recoverTs = null;
        } else {
            duration = recoverTs - occurTs;
        }
        FailureLevelWithBoundary rule = rulesManager.matchRule(duration);
        if (rule == null) {
            return;
        }

        try {
            FailureInstance failure = FailureInstance.builder()
                    .failureDefId(failureDefId)
                    .appInstanceId(appInstanceId)
                    .appComponentInstanceId(appComponentInstanceId)
                    .incidentId(incidentInstanceId)
                    .name(appName + "发生" + rule.getLevel().name() + "故障")
                    .level(rule.getLevel().name())
                    .occurTs(occurTs)
                    .recoveryTs(recoverTs)
                    .content(String.format("异常持续时长[%s分钟], 满足%s, 故障原因[%s]", duration/1000/60, rule, cause))
                    .build();

            // 推送故障实例
            try {
                String response = HttpClientsUtil.post(httpClient,
                        PropertiesUtil.getProperty("health.instance.url") + pushFailureInstancePath + "?defId=" + failureDefId,
                        JSONObject.toJSONString(failure));
                JSONObject result = JSONObject.parseObject(response);
                Integer code = result.getInteger("code");
                if (code == null || !code.equals(200)) {
                    log.error("push failure instance failed", result);
                    throw new IllegalArgumentException(response);
                }
            } catch (Throwable e) {
                log.error("push failure instance failed", e);
                throw new IllegalArgumentException(e);
            }

            collect(
                    Tuple9.of(failure.getFailureDefId(), failure.getAppInstanceId(), failure.getAppComponentInstanceId(),
                            failure.getIncidentId(), failure.getName(), failure.getLevel(), failure.getOccurTs(),
                            failure.getRecoveryTs(), failure.getContent())
            );
        } catch (Exception e) {
            log.error("check failed. failureDefId=" + failureDefId, e);
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void close() throws Exception {
        tracer.trace("DurationAlarm close========");

        if (rulesManagerFactory != null) {
            rulesManagerFactory.close();
        }
    }
}
