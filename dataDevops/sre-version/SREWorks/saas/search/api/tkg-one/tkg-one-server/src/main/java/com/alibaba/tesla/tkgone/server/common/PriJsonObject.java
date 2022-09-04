package com.alibaba.tesla.tkgone.server.common;

import com.alibaba.fastjson.JSONObject;

public class PriJsonObject {

    public static JSONObject getJSONObject(JSONObject jsonObject, String field) {
        JSONObject retJson = jsonObject.getJSONObject(field);
        if (retJson == null) {
            retJson = new JSONObject();
        }
        return retJson;
    }
}
