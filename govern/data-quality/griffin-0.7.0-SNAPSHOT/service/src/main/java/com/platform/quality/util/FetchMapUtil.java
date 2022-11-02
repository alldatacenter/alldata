package com.platform.quality.util;

import java.util.HashMap;
import java.util.Map;

/**
 * MysqlSink metadata, value等字段, 字符串转Java HashMap
 */
public class FetchMapUtil {
    public static Map<String, String> mapStringToMap(String str) {
        if (str.contains("Map(")) {
            str = str.replace("Map(", "");
            str = str.replace(")", "");
        }

        String[] strs = str.split(",");
        Map<String, String> map = new HashMap<String, String>();
        for (String string : strs) {
            String key = string.split("->")[0].trim();
            String value = string.split("->")[1].trim();
            map.put(key, value);
        }
        return map;
    }
}
