package com.elasticsearch.cloud.monitor.metric.common.utils;

import com.elasticsearch.cloud.monitor.metric.common.pojo.SearchFilterConfig;
import com.google.common.collect.Sets;

import java.util.Set;

/**
 * @author xiaoping
 * @date 2020/6/10
 */
public class WildCardUtil {

    private static Set<Character> ESCAPES = Sets.newHashSet('$', '^', '[', ']', '(', ')', '{',
        '|', '+', '\\', '.', '<', '>');

    public static boolean match(String pattern, String input) {
        if (input == null) {
            return false;
        }
        String regexp = wildcardToRegexp(pattern);
        return input.matches(regexp);
    }

    private static String wildcardToRegexp(String pattern) {
        String result = "^";

        for (int i = 0; i < pattern.length(); i++) {
            char ch = pattern.charAt(i);
            boolean isEscaped = false;

            if (ESCAPES.contains(ch)) {
                result += "\\" + ch;
                isEscaped = true;
            }

            if (!isEscaped) {
                if (ch == '*') {
                    result += ".*";
                } else if (ch == '?') {
                    result += ".";
                } else {
                    result += ch;
                }
            }
        }
        result += "$";
        return result;
    }

    public static boolean matchMetricWhiteList(SearchFilterConfig searchFilterConfig, String metricName) {
        if (searchFilterConfig == null) {
            return true;
        }
        if (searchFilterConfig.getWhiteMetric() != null) {
            for (String pattern : searchFilterConfig.getWhiteMetric()) {
                if (match(pattern, metricName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
