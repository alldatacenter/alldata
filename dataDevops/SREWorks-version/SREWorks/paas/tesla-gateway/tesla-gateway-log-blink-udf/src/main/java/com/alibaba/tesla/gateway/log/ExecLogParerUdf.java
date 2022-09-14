package com.alibaba.tesla.gateway.log;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.gateway.log.domain.ExecLog;
import com.alibaba.tesla.gateway.log.util.LogParserTimeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.table.functions.TableFunction;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将日志拆解开
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class ExecLogParerUdf extends TableFunction<ExecLog> {

    public static final String REG = ".*(actionName=executeLog.*)";
    public static final Pattern pattern = Pattern.compile(REG);

    public static final Pattern FROM_PATTERN_1 = Pattern.compile("X-BIZ-FLAG=(\\w+)");
    public static final Pattern FROM_PATTERN_2 = Pattern.compile("X-BIZ-FLAG%3D(w+)");

    public static final String X_FROM = "X-BIZ-FLAG";

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
            json.put(kv[0], kv.length == 1 ? "" : kv[1]);
        }
        ExecLog execLog = JSONObject.parseObject(json.toJSONString(), ExecLog.class);
        if(execLog != null){
            String createTime = getCreateTime(content);
            execLog.setCreateTime(createTime);
            if(json.get("x-env") != null && !Objects.equals("null", json.getString("x-env"))){
                execLog.setXEnv(json.getString("x-env").toLowerCase());
            }else {
                execLog.setXEnv("prod");
            }
            if(json.get("method") != null){
                execLog.setHttpMethod(json.getString("method"));
            }
            if (json.get("rawQuery") != null && json.getString("rawQuery").contains(X_FROM)) {
                String rawQuery = json.getString("rawQuery");
                String s = matchFrom(FROM_PATTERN_1, rawQuery);
                if (StringUtils.isBlank(s)) {
                    s = matchFrom(FROM_PATTERN_2, rawQuery);
                }
                execLog.setSourceFlag(s);
            }
            if (json.get("headers") != null) {
                try {
                    JSONObject headers = JSONObject.parseObject(json.getString("headers"));
                    if (headers.get("x-empid") != null) {
                        execLog.setEmpId(JSONArray.parseArray(headers.getString("x-empid")).getString(0));
                    }
                }catch (Exception e) {

                }

            }
            collect(execLog);
        }
    }

    protected String matchFrom(Pattern pattern, String content){
        Matcher matcher = pattern.matcher(content);
        if(matcher.find()){
            return matcher.group(1);
        }
        return null;
    }

    private String getCreateTime(String content) {
        return LogParserTimeUtils.parserTime(content);
    }


}
