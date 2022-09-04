package com.elasticsearch.cloud.monitor.metric.common.blink.utils;

import com.elasticsearch.cloud.monitor.metric.common.datapoint.ImmutableDataPoint;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class Filter {

    public static volatile List<String> list = new ArrayList<String>();

    public static ImmutableDataPoint filterMetric(ImmutableDataPoint dataPoint, boolean include) {

        if (StringUtils.isNotEmpty(dataPoint.getName())) {
            boolean match = match(dataPoint.getName());
            if (match ^ include) {
                return null;
            } else {
                return dataPoint;
            }
        } else {
            int before = dataPoint.getMetrics().size();
            List<String> metrics = new ArrayList<>(dataPoint.getMetrics().size());
            List<Double> values = new ArrayList<>(dataPoint.getMetrics().size());
            for (int i = 0; i < dataPoint.getMetrics().size(); i++) {
                boolean match = match(dataPoint.getMetrics().get(i));
                if (match ^ include) {

                } else {
                    metrics.add(dataPoint.getMetrics().get(i));
                    values.add(dataPoint.getValues().get(i));
                }
            }
            if (before == metrics.size()) {
                return dataPoint;
            }
            if (metrics.size() == 0) {
                return null;
            } else if (metrics.size() == 1) {
                return new ImmutableDataPoint(metrics.get(0), dataPoint.getTimestamp(), values.get(0),
                    dataPoint.getTags(),
                    dataPoint.getGranularity());
            } else {
                return new ImmutableDataPoint(metrics, values, dataPoint.getTimestamp(), dataPoint.getTags(),
                    dataPoint.getGranularity());
            }
        }

    }

    public static boolean match(String metric) {
        if (list.size() == 0) {
            return false;
        } else {
            for (String item : list) {
                if (item.equals(metric) || match(item, metric)) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("Duplicates")
    public static boolean match(String pattern, String str) {
        if (pattern == null || str == null) {
            return false;
        }

        boolean result = false;
        char c; // 当前要匹配的字符串
        boolean beforeStar = false; // 是否遇到通配符*
        int back_i = 0;// 回溯,当遇到通配符时,匹配不成功则回溯
        int back_j = 0;
        int i, j;
        for (i = 0, j = 0; i < str.length(); ) {
            if (pattern.length() <= j) {
                if (back_i != 0) {// 有通配符,但是匹配未成功,回溯
                    beforeStar = true;
                    i = back_i;
                    j = back_j;
                    back_i = 0;
                    back_j = 0;
                    continue;
                }
                break;
            }

            if ((c = pattern.charAt(j)) == '*') {
                if (j == pattern.length() - 1) {// 通配符已经在末尾,返回true
                    result = true;
                    break;
                }
                beforeStar = true;
                j++;
                continue;
            }

            if (beforeStar) {
                if (str.charAt(i) == c) {
                    beforeStar = false;
                    back_i = i + 1;
                    back_j = j;
                    j++;
                }
            } else {
                if (c != '?' && c != str.charAt(i)) {
                    result = false;
                    if (back_i != 0) {// 有通配符,但是匹配未成功,回溯
                        beforeStar = true;
                        i = back_i;
                        j = back_j;
                        back_i = 0;
                        back_j = 0;
                        continue;
                    }
                    break;
                }
                j++;
            }
            i++;
        }

        if (i == str.length() && j == pattern.length())// 全部遍历完毕
        {
            result = true;
        }
        return result;
    }

    public static List<String> getList() {
        return list;
    }

    public static void setList(List<String> list) {
        Filter.list = list;
    }
}
