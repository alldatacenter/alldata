package com.alibaba.sreworks.dataset.processors;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.util.TypeUtils;
import com.alibaba.sreworks.dataset.common.type.ColumnType;
import com.alibaba.sreworks.dataset.domain.bo.*;
import com.alibaba.sreworks.dataset.domain.primary.InterfaceConfig;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * 处理器抽象类
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/27 15:37
 */
public abstract class AbstractProcessor {
    protected static final String SLASH = "/";

    protected static final String COMMA = ",";

    protected static final String REGEX_DOT = "\\.";

    public static final String PAGE_NUM_KEY = "pageNum";

    public static final String PAGE_SIZE_KEY = "pageSize";

    protected static final String TOTAL_NUM_KEY = "totalNum";

    protected static final String DATA_KEY = "datas";

    protected static final int DEFAULT_PAGE_NUM = 1;

    protected static final int DEFAULT_PAGE_SIZE = 100;

    abstract Map<String, Object> getModelData(InterfaceConfig interfaceConfig, Map<String, Object> params) throws Exception;

    protected Object getValue(Object rawValue, ColumnType paramType) {
        Object value;
        switch (paramType) {
            case DOUBLE:
                value = TypeUtils.castToDouble(rawValue);
                break;
            case STRING:
                value = TypeUtils.castToString(rawValue);
                break;
            case LONG:
                value = TypeUtils.castToLong(rawValue);
                break;
            case INT:
                value = TypeUtils.castToInt(rawValue);
                break;
            case FLOAT:
                value = TypeUtils.castToFloat(rawValue);
                break;
            case BOOLEAN:
                value = TypeUtils.castToBoolean(rawValue);
                break;
            default:
                value = rawValue;
        }

        return value;
    }

    protected String[] parseStrFieldAsArr(String fields, String separator) {
        if (StringUtils.isEmpty(fields)) {
            return new String[0];
        } else {
            return fields.split(separator);
        }
    }

    protected List<String> parseStrFieldAsList(String fields, String separator) {
        if (StringUtils.isEmpty(fields)) {
            return new ArrayList<>();
        } else {
            return Arrays.asList(fields.split(separator));
        }
    }

    protected List<InterfaceQueryField> parseInterfaceQueryField(String queryField) {
        return JSONArray.parseArray(queryField, InterfaceQueryField.class);
    }

    protected List<InterfaceGroupField> parseInterfaceGroupField(String groupField) {
        return JSONArray.parseArray(groupField, InterfaceGroupField.class);
    }

    protected List<InterfaceSortField> parseInterfaceSortField(String sortField) {
        return JSONArray.parseArray(sortField, InterfaceSortField.class);
    }

    protected List<String> parseInterfaceResponseParam(String responseParam) {
        return JSONArray.parseArray(responseParam, String.class);
    }


    protected List<DataModelValueField> parseModelValueField(String modelValueFields) {
        return JSONArray.parseArray(modelValueFields, DataModelValueField.class);
    }

    protected List<DataModelGroupField> parseModelGroupField(String modelGroupFields) {
        return JSONArray.parseArray(modelGroupFields, DataModelGroupField.class);
    }

    protected List<DataInterfaceSortField> parseSortField(String sortFields) {
        return JSONArray.parseArray(sortFields, DataInterfaceSortField.class);
    }

    protected Map<String, Object> buildRetData(List<JSONObject> datas, Map<String, Object> params, boolean paging) {
        Map<String, Object> results = new HashMap<>();
        if (paging) {
            results.put(PAGE_NUM_KEY, Integer.parseInt(String.valueOf(params.getOrDefault(PAGE_NUM_KEY, DEFAULT_PAGE_NUM))));
            results.put(PAGE_SIZE_KEY, Integer.parseInt(String.valueOf(params.getOrDefault(PAGE_SIZE_KEY, DEFAULT_PAGE_SIZE))));
        }
        results.put(TOTAL_NUM_KEY, datas.size());
        results.put(DATA_KEY, datas);

        return results;
    }
}
