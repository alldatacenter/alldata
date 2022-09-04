package com.alibaba.sreworks.common.util;

import java.lang.reflect.InvocationHandler;
import java.util.List;

import com.alibaba.fastjson.JSONObject;

import com.google.common.base.CaseFormat;
import org.springframework.stereotype.Service;

/**
 * @author jinghua.yjh
 */
@Service
public class RegularUtil {

    public static void gmt2Date(JSONObject jsonObject) {
        if (jsonObject == null) {
            return;
        }
        for (String key : jsonObject.keySet().toArray(new String[0])) {
            if (!key.startsWith("gmt")) {
                continue;
            }
            try {
                String newKey = key.replace("gmt", "date");
                Long value = jsonObject.getLong(key);
                if (value > System.currentTimeMillis() / 100) {
                    value = value / 1000;
                }
                jsonObject.put(newKey, TimeUtil.timeStamp2Date(value));
            } catch (Exception ignored) {
            }
        }
    }

    public static void gmt2Date(List<JSONObject> list) {
        if (list == null) {
            return;
        }
        list.forEach(RegularUtil::gmt2Date);
    }

    public static void underscoreToCamel(JSONObject jsonObject) {
        if (jsonObject == null) {
            return;
        }
        for (String key : jsonObject.keySet().toArray(new String[0])) {
            String newKey = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, key);
            jsonObject.put(newKey, jsonObject.get(key));
            if (!newKey.equals(key)) {
                jsonObject.remove(key);
            }
        }
    }

    public static void underscoreToCamel(List<JSONObject> list) {
        if (list == null) {
            return;
        }
        list.forEach(RegularUtil::underscoreToCamel);
    }

    public static <T> void changeValue(JSONObject jsonObject, String key, Class<T> clazz) {
        jsonObject.put(key, JSONObject.parseObject(jsonObject.getString(key), clazz));
    }

    public static <T> void changeValue(List<JSONObject> list, String key, Class<T> clazz) {
        for (JSONObject jsonObject : list) {
            changeValue(jsonObject, key, clazz);
        }
    }

}                                                                                      
