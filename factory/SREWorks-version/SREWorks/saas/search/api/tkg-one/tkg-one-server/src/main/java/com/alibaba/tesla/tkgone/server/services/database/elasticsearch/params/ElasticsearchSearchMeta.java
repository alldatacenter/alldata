package com.alibaba.tesla.tkgone.server.services.database.elasticsearch.params;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.tkgone.server.common.Tools;
import lombok.Data;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author yangjinghua
 */
@Data
@SuppressWarnings("unused")
public class ElasticsearchSearchMeta {

    private JSONObject metaJson;
    private JSONObject queryJson;

    private String searchType;
    private String aggType;
    private List<String> types = new ArrayList<>();
    private String aggField;
    private Long aggSize;

    private Long from;
    private Long size;
    private Long to;
    private Long factor;
    private Long interval;

    public ElasticsearchSearchMeta(JSONObject metaJson, JSONObject queryJson) {

        if (metaJson == null) { metaJson = new JSONObject(); }
        if (queryJson == null) { queryJson = new JSONObject(); }
        setMetaJson(metaJson);
        setQueryJson(queryJson);
    }

    public String getSearchType() {
        if (metaJson.containsKey("aggField")) {
            return "agg";
        } else {
            return "search";
        }
    }

    public String getAggType() {
        if (metaJson.get("aggType") == null) {
            return "distinct";
        } else {
            return metaJson.getString("aggType");
        }
    }

    public List<String> getTypes() {
        List<String> retList = new ArrayList<>();
        if (metaJson.containsKey("types")) {
            retList.addAll(metaJson.getJSONArray("types").toJavaList(String.class));
        }
        if (metaJson.containsKey("type")) {
            Object type = JSONObject.toJSON(metaJson.get("type"));
            if (type instanceof JSONArray) {
                retList.addAll(metaJson.getJSONArray("type").toJavaList(String.class));
            } else {
                retList.add(metaJson.getString("type"));
            }
        }
        retList.removeAll(Arrays.asList(null, ""));
        return retList;
    }

    public String getAggField() {
        return metaJson.getString("aggField");
    }

    public Long getFrom() {
        String from = metaJson.getString("from");
        if (from == null) {
            if (metaJson.containsKey("page") && metaJson.containsKey("size")) {
                return (metaJson.getLongValue("page") - 1) * metaJson.getLongValue("size");
            }
            return 0L;
        }
        switch (getSearchType()) {
            case "search":
                return Long.parseLong(from);
            case "agg":
                Long f;
                if (StringUtils.isNumeric(from)) {
                    f = Long.parseLong(from);
                } else {
                    if (from.startsWith("query")) {
                        from = (String)Tools.getValueByPath(from.replaceFirst("query\\.", ""), queryJson);
                    }
                    f = Long.parseLong(Tools.date2TimeStamp(from));
                }
                return f * getFactor();
            default:
                return Long.parseLong(from);
        }
    }

    public Long getSize() {
        Long size = metaJson.getLong("size");
        return size == null ? 10L : size;
    }

    public Long getTo() {
        String to = metaJson.getString("to");
        if (to == null) {
            return 0L;
        }
        switch (getSearchType()) {
            case "search":
                return Long.parseLong(to);
            case "agg":
                Long t;
                if (StringUtils.isNumeric(to)) {
                    t = Long.parseLong(to);
                } else {
                    if (to.startsWith("query")) {
                        to = (String)Tools.getValueByPath(to.replaceFirst("query\\.", ""), queryJson);
                    }
                    t = Long.parseLong(Tools.date2TimeStamp(to));
                }
                return t * getFactor();
            default:
                return Long.parseLong(to);
        }
    }

    public Long getFactor() {
        Long factor = metaJson.getLong("factor");
        return factor == null ? 1L : factor;
    }

    public Long getInterval() {
        Long interval = metaJson.getLong("interval");
        return interval == null ? 300L : interval;
    }

    public Long getAggSize() {
        Long aggSize = metaJson.getLong("aggSize");
        return aggSize == null ? 10L : aggSize;
    }

}
