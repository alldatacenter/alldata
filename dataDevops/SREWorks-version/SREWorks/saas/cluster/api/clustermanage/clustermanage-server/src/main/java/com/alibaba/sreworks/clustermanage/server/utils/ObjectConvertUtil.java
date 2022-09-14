package com.alibaba.sreworks.clustermanage.server.utils;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class ObjectConvertUtil {

    /**
     * text to objct
     *
     * @param text
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T from(String text, Class<T> clazz) {
        return JSONObject.parseObject(text, clazz);
    }

    /**
     * str to object list
     *
     * @param text
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> List<T> froms(String text, Class<T> clazz) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        return JSONObject.parseArray(text, clazz);
    }

    /**
     * to json string
     *
     * @param <T>
     * @return
     */
    public static <T> String toJsonString(T t) {
        if (t == null) {
            return null;
        }
        return JSONObject.toJSONString(t);
    }

    /**
     * 将一个对象转换为一个 Json Object
     *
     * @param t 要转换的对象
     * @return JSONObject
     */
    public static <T> JSONObject toJsonObject(T t) {
        if (t == null) {
            return null;
        }
        return JSONObject.parseObject(JSONObject.toJSONString(t));
    }
}
