package com.elasticsearch.cloud.monitor.metric.common.utils;

import com.elasticsearch.cloud.monitor.metric.common.rule.filter.TagVFilter;
import java.util.*;

public class FilterUtils {
    public static Map<String, TagVFilter> getFilterMap(Map<String, String> tags){
        Map<String, TagVFilter> filters = new HashMap<>();
        List<TagVFilter> tagsFilterList = new LinkedList<>();
        TagVFilter.tagsToFilters(tags, tagsFilterList);
        for (TagVFilter tagVFilter : tagsFilterList) {
            filters.put(tagVFilter.getTagk(), tagVFilter);
        }
        return filters;
    }

    public static String getFilters(final Map<String, TagVFilter> filterMap) {
        if (filterMap == null) {
            return "";
        }
        Map<String, TagVFilter> sortMap = new TreeMap<>(filterMap);
        char sep = ',';
        StringBuilder tagSb = new StringBuilder();
        StringBuilder noGroupBySb = new StringBuilder();
        for (TagVFilter filter : sortMap.values()) {
            StringBuilder sb = new StringBuilder();
            sb.append(filter.getTagk()).append("=").append(filter.getFilter());
            if (filter.isGroupBy()) {
                tagSb.append(sb.toString()).append(sep);
            } else {
                noGroupBySb.append(sb.toString()).append(sep);
            }
        }

        if (tagSb.length() > 0) {
            tagSb.deleteCharAt(tagSb.length() - 1);
        }
        if (noGroupBySb.length() > 0) {
            noGroupBySb.deleteCharAt(noGroupBySb.length() - 1);
            noGroupBySb.insert(0, "filter:");
        }
        if (tagSb.length() > 0 && noGroupBySb.length() > 0) {
            tagSb.append(";");
        }
        return tagSb.append(noGroupBySb).toString();
    }

    public static boolean matchTags(Map<String, String> dpTags, Map<String, TagVFilter> filterMap){
        boolean find = true;
        for (Map.Entry<String, TagVFilter> entry : filterMap.entrySet()) {
            String tagK = entry.getKey();
            String tagV = dpTags.get(tagK);
            if (tagV != null) {
                TagVFilter filter = entry.getValue();
                if (!filter.match(dpTags)) {
                    find = false;
                    break;
                }
            } else {
                find = false;
                break;
            }
        }

        return find;
    }

}
