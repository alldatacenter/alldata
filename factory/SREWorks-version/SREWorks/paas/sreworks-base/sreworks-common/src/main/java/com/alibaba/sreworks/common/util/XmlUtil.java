package com.alibaba.sreworks.common.util;

import com.alibaba.fastjson.JSONObject;

import org.json.XML;

public class XmlUtil {

    public static String toJsonString(String xml) {
        return XML.toJSONObject(xml).toString();
    }

    public static JSONObject toJsonObject(String xml) {
        return JSONObject.parseObject(toJsonString(xml));
    }

}
