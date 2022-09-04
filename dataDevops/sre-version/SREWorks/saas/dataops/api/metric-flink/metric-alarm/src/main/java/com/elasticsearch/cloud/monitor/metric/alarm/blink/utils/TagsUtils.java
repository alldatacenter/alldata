package com.elasticsearch.cloud.monitor.metric.alarm.blink.utils;

import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author xingming.xuxm
 * @Date 2019-12-12
 */
public class TagsUtils {
    public final static String TAGS_SEPARATOR = ",";
    public final static String TAGS_KEY_VALUE_SEPARATOR = "=";

    public static Map<String, String>  toTagsMap(String tagStr) {
        Map<String, String> tags = new HashMap<>(16);
        if (StringUtils.isNotEmpty(tagStr)) {
            String[] tagArray = tagStr.split(TAGS_SEPARATOR);
            for (String tag : tagArray) {
                String[] kv = tag.split(TAGS_KEY_VALUE_SEPARATOR, 2);
                tags.put(kv[0], kv[1]);
            }
        }
        return tags;
    }
}
