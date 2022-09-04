package com.elasticsearch.cloud.monitor.metric.common.blink.udf;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.flink.table.functions.ScalarFunction;
import org.springframework.util.DigestUtils;


/**
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2022/06/30 17:09
 */
public class GeneratePmdbMetricDataIdUdf extends ScalarFunction {
    private static final Log log = LogFactory.getLog(ExtractTimeUdf.class);

    public String eval(Integer metricId, String labels, Long timestamp) {
        return DigestUtils.md5DigestAsHex((metricId + "|" + JSONObject.toJSONString(labels) + "|" + timestamp).getBytes());
    }

//    public String eval(Integer metricId, String labels) {
//        return DigestUtils.md5DigestAsHex((metricId + "|" + JSONObject.toJSONString(labels)).getBytes());
//    }
}
