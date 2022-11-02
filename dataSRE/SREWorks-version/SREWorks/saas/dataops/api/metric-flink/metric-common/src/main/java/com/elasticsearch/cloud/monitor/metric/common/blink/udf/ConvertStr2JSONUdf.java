package com.elasticsearch.cloud.monitor.metric.common.blink.udf;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.flink.table.functions.ScalarFunction;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2022/06/30 17:09
 */
public class ConvertStr2JSONUdf extends ScalarFunction {
    private static final Log log = LogFactory.getLog(ExtractTimeUdf.class);

    public Map<String, String> eval(String str) {
        if (StringUtils.isBlank(str)) {
            return null;
        }
        JSONObject r = JSONObject.parseObject(str);
        Map<String, String> json = new HashMap();
        for(String key : r.keySet()) {
            json.put(key, r.getString(key));
        }
        return json;
    }
}
