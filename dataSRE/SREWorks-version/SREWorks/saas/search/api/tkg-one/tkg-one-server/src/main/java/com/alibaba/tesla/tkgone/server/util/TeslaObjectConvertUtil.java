package com.alibaba.tesla.tkgone.server.util;

import com.alibaba.fastjson.JSONObject;

import java.util.List;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class TeslaObjectConvertUtil {

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

}
