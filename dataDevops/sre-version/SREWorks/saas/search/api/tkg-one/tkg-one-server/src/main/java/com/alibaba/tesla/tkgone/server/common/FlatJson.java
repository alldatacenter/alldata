package com.alibaba.tesla.tkgone.server.common;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yangjinghua
 */
public class FlatJson {

    public static JSONObject jsonFormatter(JSONObject jsonObject) throws Exception {
        return JSONObject.parseObject(jsonFormatter(JSONObject.toJSONString(jsonObject)));
    }

    public static String jsonFormatter(String uglyJSONString) throws Exception {

        Map<String, Object> map = new HashMap<>(0);
        parseJson2Map(map, uglyJSONString);
        JSONObject jsonObject = new JSONObject(map);
        uglyJSONString = jsonObject.toString();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(uglyJSONString);
        return gson.toJson(je);
    }

    private static void parseJson2Map(Map<String, Object> map, String json) throws Exception {
        JsonElement jsonElement = new JsonParser().parse(json);
        if (jsonElement.isJsonObject()) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            parseJson2Map(map, jsonObject, null);
        } else if (jsonElement.isJsonArray()) {
            JsonArray jsonArray = jsonElement.getAsJsonArray();
            for (JsonElement aJsonArray : jsonArray) {
                parseJson2Map(map, aJsonArray.getAsJsonObject(), null);
            }
        } else if (jsonElement.isJsonPrimitive()) {
            throw new Exception("please check the json format!");
        } else {
            jsonElement.isJsonNull();
        }
    }

    private static void parseJson2Map(Map<String, Object> map, JsonObject jsonObject, String parentKey) {
        for (Map.Entry<String, JsonElement> object : jsonObject.entrySet()) {
            String key = object.getKey();
            JsonElement value = object.getValue();
            String fullKey = (null == parentKey || "".equals(parentKey.trim())) ? key : parentKey.trim() + "." + key;
            if (value.isJsonNull()) {
                map.put(fullKey, null);
            } else if (value.isJsonObject()) {
                parseJson2Map(map, value.getAsJsonObject(), fullKey);
            } else if (value.isJsonArray()) {
                JsonArray jsonArray = value.getAsJsonArray();
                for (JsonElement element : jsonArray) {
                    parseJson2Map(map, element.getAsJsonObject(), fullKey);
                }
            } else if (value.isJsonPrimitive()) {
                try {
                    JsonElement element = new JsonParser().parse(value.getAsString());
                    if (element.isJsonNull()) {
                        map.put(fullKey, value.getAsString());
                    } else if (element.isJsonObject()) {
                        parseJson2Map(map, element.getAsJsonObject(), fullKey);
                    } else if (element.isJsonPrimitive()) {
                        JsonPrimitive jsonPrimitive = element.getAsJsonPrimitive();

                        if (jsonPrimitive.isNumber()) {
                            map.put(fullKey, jsonPrimitive.getAsNumber());
                        } else {
                            map.put(fullKey, jsonPrimitive.getAsString());
                        }
                    } else if (element.isJsonArray()) {
                        JsonArray jsonArray = element.getAsJsonArray();
                        for (JsonElement jsonElement : jsonArray) {
                            parseJson2Map(map, jsonElement.getAsJsonObject(), fullKey);
                        }
                    }
                } catch (Exception e) {
                    map.put(fullKey, value.getAsString());
                }
            }
        }
    }
}
