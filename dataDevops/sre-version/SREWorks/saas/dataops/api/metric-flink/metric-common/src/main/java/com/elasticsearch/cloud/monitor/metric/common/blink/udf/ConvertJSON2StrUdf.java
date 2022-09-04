package com.elasticsearch.cloud.monitor.metric.common.blink.udf;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.flink.table.functions.ScalarFunction;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2022/06/30 17:09
 */
public class ConvertJSON2StrUdf extends ScalarFunction {
    private static final Log log = LogFactory.getLog(ExtractTimeUdf.class);

    public String eval(Map<String, String> json) {
        if (Objects.nonNull(json)) {
            return JSONObject.toJSONString(json);
        } else {
            return null;
        }
    }
}
