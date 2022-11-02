package com.alibaba.tdata.aisp.server.common.utils;

import com.alibaba.fastjson.JSONObject;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * @ClassName: JsonUtil
 * @Author: dyj
 * @DATE: 2021-04-01
 * @Description:
 **/
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
}
