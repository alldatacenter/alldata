package com.alibaba.tesla.gateway.log;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.gateway.log.domain.VerifyTeslaHeaderResultLog;
import com.alibaba.tesla.gateway.log.util.LogParserTimeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.table.functions.TableFunction;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class VerifyTeslaHeadersErrorParerUdf extends TableFunction<VerifyTeslaHeaderResultLog> {

    public static final String REG = ".*(gatewayAuthCheck:verifyTeslaHeadersError.*)";
    public static final Pattern pattern = Pattern.compile(REG);

    protected String match(String content){
        Matcher matcher = pattern.matcher(content);
        if(matcher.find()){
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 处理逻辑
     * @param content
     */
    public void eval(String content){
        String value = match(content);
        if(StringUtils.isEmpty(value)){
            return;
        }
        String[] strs = value.split("\\|\\|");
        JSONObject json = new JSONObject();
        for (String str : strs) {
            String[] kv = str.split("=", 2);
            if (kv.length > 1) {
                json.put(kv[0], kv[1]);
            }
        }

        VerifyTeslaHeaderResultLog resultLog = new VerifyTeslaHeaderResultLog();
        resultLog.setContent(value);

        Object requestId = json.get("requestId");
        if (requestId instanceof String) {
            resultLog.setRequestId((String)requestId);
        }

        Object requestPath = json.get("requestPath");
        if (requestPath instanceof String) {
            resultLog.setRequestPath((String) requestPath);
        }

        String dateTime = getCreateTime(content);
        if (dateTime != null) {
            resultLog.setCreateTime(dateTime);
        }

        Object routeId = json.get("routeId");
        if (requestId instanceof String) {
            resultLog.setRouteId((String) routeId);
        }

        collect(resultLog);
    }

    private String getCreateTime(String content) {
        return LogParserTimeUtils.parserTime(content);
    }
}
