package com.alibaba.tesla.tkgone.server.common;

import java.util.HashMap;
import java.util.Map;

/**
 * 封装一些对于Map常见的操作工具
 *
 * @author feiquan
 */
@SuppressWarnings("unchecked")
public class MapUtil {
    /**
     * 快速建立一个Map
     *
     * @param fields
     * @return
     */
    public static Map<String, Object> create(Object... fields) {
        if (fields.length % 2 == 1) {
            throw new IllegalArgumentException("Fields must be paired");
        }
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < fields.length; i++) {
            String key = fields[i++].toString();
            Object value = fields[i];
            map.put(key, value);
        }
        return map;
    }

    /**
     * 根据path的路径获取字符串值
     *
     * @param object
     * @param path
     * @return
     */
    public static String checkString(Map<String, Object> object, String path) {
        return checkObject(object, path).toString();
    }

    /**
     * 根据path的路径获取对象值
     *
     * @param object
     * @param path
     * @return
     */
    public static Object checkObject(Map<String, Object> object, String path) {
        Object value = getObject(object, path);
        if (value == null) {
            throw new IllegalArgumentException("Path[" + path + "] not exists");
        }
        return value;
    }

    /**
     * 根据path的路径获取整形值
     *
     * @param object
     * @param path
     * @return
     */
    public static int checkInteger(Map<String, Object> object, String path) {
        Object value = checkObject(object, path);
        if (value instanceof Number) {
            return ((Number)value).intValue();
        } else {
            try {
                return Integer.parseInt(object.toString());
            } catch (NumberFormatException e) {
                throw new NumberFormatException(String.format("属性(%s=%s)值非整型数", path, value));
            }
        }
    }

    /**
     * 获取多级map的属性
     *
     * @param object
     * @param path
     * @return
     */
    public static String getString(Map<String, Object> object, String path) {
        Object value = getObject(object, path);
        return value == null ? null : value.toString();
    }

    /**
     * 获取多级map的属性
     *
     * @param map
     * @param path
     * @return
     */
    public static Object getObject(Map<String, Object> map, String path) {
        if (path.indexOf('.') > 0) {
            return getObject(map, splitKeys(path));
        } else {
            return map.get(path);
        }
    }

    /**
     * 获取多级map的属性
     *
     * @param map
     * @param keys
     * @return
     */
    public static Object getObject(Map<String, Object> map, String... keys) {
        Object result = null;
        for (int i = 0; i < keys.length; i++) {
            result = map.get(keys[i]);
            if (result instanceof Map) {
                map = (Map<String, Object>)result;
            } else if (i + 1 < keys.length) {
                result = null;
                break;
            }
        }
        return result;
    }

    private static String[] splitKeys(String path) {
        return path.trim().split("\\.");
    }
}
