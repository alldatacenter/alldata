package com.alibaba.sreworks.job.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;

/**
 * @author jinghua.yjh
 */
public class JsonUtil {

    public static JSONObject map(Object... args) {
        JSONObject jsonObject = new JSONObject();
        for (int index = 0; index < args.length; index += 2) {
            jsonObject.put((String)args[index], args[index + 1]);
        }
        return jsonObject;
    }

    public static JSONArray list(Object... args) {
        return new JSONArray(Arrays.asList(args));
    }

    public static void mergeJsonArray(JSONArray j1, JSONArray j2) {
        try {
            for (int i = 0; i < j1.size(); i++) {
                Object v1 = j1.get(i);
                Object v2 = j2.get(i);
                mergeJsonObject((JSONObject)v1, (JSONObject)v2);
            }
        } catch (Exception ignored) {
        }
    }

    public static void mergeJsonObject(JSONObject j1, JSONObject j2) {
        if (CollectionUtils.isEmpty(j2)) {
            return;
        }
        for (String key : j1.keySet()) {
            if (j2.containsKey(key)) {
                Object v1 = JSON.toJSON(j1.get(key));
                Object v2 = JSON.toJSON(j2.get(key));
                if (v1 instanceof JSONObject && v2 instanceof JSONObject) {
                    mergeJsonObject((JSONObject)v1, (JSONObject)v2);
                } else if (v1 instanceof JSONArray && v2 instanceof JSONArray) {
                    mergeJsonArray((JSONArray)v1, (JSONArray)v2);
                } else {
                    j1.put(key, v2);
                }
            }
        }
        for (String key : j2.keySet()) {
            if (!j1.containsKey(key)) {
                j1.put(key, j2.get(key));
            }
        }
    }

}
