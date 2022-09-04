package com.elasticsearch.cloud.monitor.metric.common.blink.udtf;

import com.alibaba.fastjson.JSONObject;
import com.elasticsearch.cloud.monitor.metric.common.blink.udf.ExtractTimeUdf;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.table.functions.TableFunction;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;


/**
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2022/06/30 17:09
 */
public class MergePmdbMetricLabelsUdtf extends TableFunction<Tuple2<String, String>> {
    private static final Log log = LogFactory.getLog(ExtractTimeUdf.class);

    public void eval(Integer metricId, String insLabels, String metricLabels) {
        JSONObject labels = new JSONObject();
        if (StringUtils.isNotBlank(metricLabels)) {
            JSONObject metric = JSONObject.parseObject(metricLabels);
            if (!CollectionUtils.isEmpty(metric)) {
                labels.putAll(metric);
            }
        }

        if (StringUtils.isNotBlank(insLabels)) {
            JSONObject ins = JSONObject.parseObject(insLabels);
            if (!CollectionUtils.isEmpty(ins)) {
                labels.putAll(ins);
            }
        }

        String uid = DigestUtils.md5DigestAsHex((metricId + "|" + JSONObject.toJSONString(labels)).getBytes());

        collect(
                Tuple2.of(uid, JSONObject.toJSONString(labels))
        );
    }
}
