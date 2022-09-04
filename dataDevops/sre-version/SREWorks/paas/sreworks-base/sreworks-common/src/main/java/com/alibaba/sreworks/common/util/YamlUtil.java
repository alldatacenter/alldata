package com.alibaba.sreworks.common.util;

import java.util.Map;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.checkerframework.checker.units.qual.K;

public class YamlUtil {

    public static String toJson(String yaml) throws JsonProcessingException {
        if (StringUtil.isEmpty(yaml)) {
            return "";
        }
        ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
        Object obj = yamlReader.readValue(yaml, Object.class);

        ObjectMapper jsonWriter = new ObjectMapper();
        return jsonWriter.writeValueAsString(obj);
    }

    public static <T> T toObject(String yaml, Class<T> clazz) throws JsonProcessingException {
        return JSONObject.parseObject(toJson(yaml), clazz);
    }

    public static JSONObject toJsonObject(String yaml) throws JsonProcessingException {
        return JSONObject.parseObject(toJson(yaml));
    }

    public static JSONArray toJsonArray(String yaml) throws JsonProcessingException {
        return JSONArray.parseArray(toJson(yaml));
    }

    public static String toYaml(String jsonString) throws JsonProcessingException {
        JsonNode jsonNodeTree = new ObjectMapper().readTree(jsonString);
        return new YAMLMapper().writeValueAsString(jsonNodeTree);
    }

    public static String toYaml(JSONObject jsonObject) throws JsonProcessingException {
        return toYaml(JSONObject.toJSONString(jsonObject));
    }

    public static <K, V> String toYaml(Map<K, V> map) throws JsonProcessingException {
        return toYaml(JSONObject.toJSONString(map));
    }

}
