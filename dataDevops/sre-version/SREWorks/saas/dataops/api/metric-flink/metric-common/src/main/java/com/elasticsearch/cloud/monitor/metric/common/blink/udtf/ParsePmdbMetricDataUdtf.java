package com.elasticsearch.cloud.monitor.metric.common.blink.udtf;

import com.alibaba.fastjson.JSONObject;
import com.elasticsearch.cloud.monitor.metric.common.constant.MetricbeatModule;
import com.google.common.collect.ImmutableList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.table.functions.TableFunction;
import org.json.JSONTokener;
import org.springframework.util.CollectionUtils;

import java.util.*;


/**
 * SREWorks PMDB指标实例数据解析UDTF
 *
 *
 * 入参：metricbeat autodiscover metric data
 *
 * @author: fangzong.lyj
 * @date: 2022/06/09 15:17
 */
public class ParsePmdbMetricDataUdtf extends TableFunction<Tuple4<Integer, String, Float, Long>> {
    private static final Log log = LogFactory.getLog(ParsePmdbMetricDataUdtf.class);
    private ImmutableList<String> PMDB_METRIC_KEYS = ImmutableList.of("metric_id", "value", "timestamp", "labels");
    private int secondTsLen = 10;
    private int millisecondTsLen = 13;


    public void eval(String rawData) {
        JSONObject jsonData = JSONObject.parseObject(rawData);
        if (CollectionUtils.isEmpty(jsonData)) {
            return;
        }

        List<JSONObject> datas = new ArrayList<>();

        JSONObject event = jsonData.getJSONObject("event");
        String module = event.getString("module");
        MetricbeatModule metricbeatModule = MetricbeatModule.valueOf(module);
        if (metricbeatModule == MetricbeatModule.http) {
           JSONObject httpMetric = jsonData.getJSONObject("http");
           Collection<Object> rets = httpMetric.values();
            for (Object ret : rets) {
                String strMetricData = ((JSONObject)ret).getString("data");
                Object jsonMetricData = new JSONTokener(strMetricData).nextValue();
                if (jsonMetricData instanceof org.json.JSONArray) {
                    datas = JSONObject.parseArray(strMetricData, JSONObject.class);
                } else if (jsonMetricData instanceof org.json.JSONObject) {
                    datas = Collections.singletonList(JSONObject.parseObject(strMetricData));
                } else {
                    log.warn("metric data format illegal, can not parse metric data to jsonarray or jsonobject");
                }
            }
        }

        for (JSONObject data : datas) {
            if (checkMetricData(data)) {
                collect(
                        Tuple4.of(data.getInteger("metric_id"), JSONObject.toJSONString(data.getJSONObject("labels")),
                                data.getFloat("value"), data.getLong("timestamp"))
                );
            }
        }
    }

    private boolean checkMetricData(JSONObject data) {
        if (!data.containsKey("timestamp")) {
            long metricTs;
            if (data.containsKey("time")) {
                metricTs = data.getLongValue("time");
                if (String.valueOf(metricTs).length() == secondTsLen) {
                    metricTs = metricTs * 1000L;
                }
            } else if (data.containsKey("ts")) {
                metricTs = data.getLongValue("ts");
                if (String.valueOf(metricTs).length() == secondTsLen) {
                    metricTs = metricTs * 1000L;
                }
            } else {
                metricTs = new Date().getTime();
                log.warn(String.format("metric data is lack of timestamp, now timestamp is used, data:%s", data));
            }
            data.put("timestamp", metricTs);
        }

        if (String.valueOf(data.getLongValue("timestamp")).length() != millisecondTsLen) {
            log.warn(String.format("metric data timestamp is illegal, please check, data:%s", data));
            return false;
        }

        // labels可以置空
        if (!data.containsKey("labels")) {
            data.put("labels", new JSONObject());
        }

        if (data.keySet().containsAll(PMDB_METRIC_KEYS)) {
            return true;
        } else {
            log.warn(String.format("metric data format illegal，valid keys:%s, data: %s", PMDB_METRIC_KEYS, data));
            return false;
        }
    }
}
