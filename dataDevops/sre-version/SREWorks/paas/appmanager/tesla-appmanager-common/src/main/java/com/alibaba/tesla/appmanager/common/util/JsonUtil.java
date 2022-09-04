package com.alibaba.tesla.appmanager.common.util;

import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.enums.ParameterValueSetPolicy;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
public class JsonUtil {

    public static JSONObject toJson(String text){
        try {
            if(StringUtils.isEmpty(text)){
                return new JSONObject();
            }
            return JSONObject.parseObject(text);
        }catch (Exception e){
            log.error("to json failed, text={}", text);
            throw e;
        }
    }


    public static JSONObject toJsonIgnoreException(String text){
        try {
            if(StringUtils.isEmpty(text)){
                return new JSONObject();
            }
            return JSONObject.parseObject(text);
        }catch (Exception e){
            log.error("to json failed, text={}", text);
            return null;
        }
    }



    public static String toJsonString(JSONObject json) {
        if (json == null){
            return StringUtils.EMPTY;
        }
        return json.toJSONString();
    }

    public static String emptyJsonString(){
        return new JSONObject().toJSONString();
    }

    /**
     * 递归参数设置
     *
     * @param parameters 需要设置的参数字典
     * @param nameList   名称列表
     * @param value      值
     */
    public static void recursiveSetParameters(
        JSONObject parameters, String prefix, List<String> nameList, Object value, ParameterValueSetPolicy policy) {
        assert nameList.size() > 0;
        if (nameList.size() == 1) {
            String name = nameList.get(0);
            String actualName = StringUtils.isEmpty(prefix) ? name : prefix + "." + name;
            if (parameters.containsKey(name)) {
                switch (policy) {
                    case IGNORE_ON_CONFLICT:
                        log.info("overwrite parameter values, name={}, value={}", actualName, value);
                        return;
                    case EXCEPTION_ON_CONFILICT:
                        throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                            String.format("conflict overwrite parameter values, name=%s, value=%s", actualName, value));
                    default:
                        break;
                }
            }
            try {
                Object obj = JSON.parse(String.valueOf(value));
                if (obj instanceof JSONObject) {
                    parameters.put(nameList.get(0), StringEscapeUtils.escapeJson(JSONObject.toJSONString(obj)));
                } else if (obj instanceof JSONArray) {
                    parameters.put(nameList.get(0), StringEscapeUtils.escapeJson(JSONArray.toJSONString(obj)));
                } else {
                    parameters.put(nameList.get(0), value);
                }
            } catch (Exception e) {
                parameters.put(nameList.get(0), value);
            }
            return;
        }

        String name = nameList.get(0);
        String nextPrefix = StringUtils.isEmpty(prefix) ? name : prefix + "." + name;
        parameters.putIfAbsent(name, new JSONObject());
        recursiveSetParameters(
            parameters.getJSONObject(name),
            nextPrefix,
            nameList.subList(1, nameList.size()),
            value,
            policy
        );
    }

    /**
     * 递归参数获取
     *
     * @param parameters 参数字典
     * @param nameList   名称列表
     * @return 实际的值
     */
    public static Object recursiveGetParameter(JSONObject parameters, List<String> nameList) {
        assert nameList.size() > 0;
        if (nameList.size() == 1) {
            return parameters.get(nameList.get(0));
        }
        String name = nameList.get(0);
        JSONObject subParameters = new JSONObject();
        try {
            subParameters = parameters.getJSONObject(name);
        } catch (Exception e){
            log.warn("action=recursiveGetParameter||cannot recursive get parameter, key:{} can not been cast to JSONObject", name);
            return null;
        }
        if (subParameters == null) {
            log.error("action=recursiveGetParameter||cannot recursive get parameter, name={}, parameters={}", name, parameters.toJSONString());
            return null;
        }
        return recursiveGetParameter(subParameters, nameList.subList(1, nameList.size()));
    }

    public static String recursiveGetString(JSONObject parameters, List<String> nameList) {
        Object o = recursiveGetParameter(parameters, nameList);
        return o==null?null:o.toString();
    }
}
