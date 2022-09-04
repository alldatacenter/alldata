package com.alibaba.sreworks.dataset.domain.req.inter;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.dataset.common.constant.Constant;
import com.alibaba.sreworks.dataset.common.constant.ValidConstant;
import com.alibaba.sreworks.dataset.common.utils.Regex;
import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据接口配置信息
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/21 17:04
 */

@ApiModel(value="创建数据接口配置")
public class InterfaceConfigCreateReq extends InterfaceConfigBaseReq{

    @Override
    public String getName() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(name), "接口名称不允许为空");
        Preconditions.checkArgument(Regex.checkDocumentByPattern(name, Constant.INTERFACE_NAME_PATTERN),
                "接口名不符合规范:" + Constant.INTERFACE_NAME_REGEX);
        return name;
    }

    @Override
    public String getAlias() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(alias), "接口别名不允许为空");
        return alias;
    }

    @Override
    public String getRequestMethod() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(requestMethod), "请求方法不允许为空");
        requestMethod = requestMethod.toUpperCase();
        Preconditions.checkArgument(Constant.REQUEST_METHODS.contains(requestMethod) , "请求方法仅支持:" + Constant.REQUEST_METHODS);
        return requestMethod;
    }

    @Override
    public Boolean getPaging() {
        return paging == null ? false : paging;
    }

    @Override
    public String getDataSourceType() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(dataSourceType) , "数据源类型不允许为空");
        Preconditions.checkArgument(ValidConstant.SOURCE_TYPE_LIST.contains(dataSourceType) , "数据源类型仅支持:" + ValidConstant.SOURCE_TYPE_LIST);
        return dataSourceType;
    }

    @Override
    public String getDataSourceId() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(dataSourceId) , "数据源不允许为空");
        return dataSourceId;
    }

    @Override
    public String getDataSourceTable() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(dataSourceTable) , "数据表不允许为空");
        return dataSourceTable;
    }

    @Override
    public String getContentType() {
        return StringUtils.isEmpty(contentType) ? Constant.DEFAULT_CONTENT_TYPE : contentType;
    }

    @Override
    public String getMode() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(mode) , "配置模式不允许为空");
        mode = mode.toLowerCase();
        Preconditions.checkArgument(Constant.INTERFACE_CONFIG_MODE.contains(mode) , "配置模式非法");
        return mode;
    }

    @Override
    public Boolean getBuildIn() {
        if (ObjectUtils.isEmpty(buildIn)) {
            return false;
        }
        return buildIn;
    }

    @Override
    public String getQlTemplate() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(qlTemplate) , "查询模板不允许为空");
        return qlTemplate;
    }

    @Override
    public InterfaceQueryFieldReq[] getQueryFields() {
        if (queryFields != null) {
            for (InterfaceQueryFieldReq req : queryFields) {
                req.validReq(dataSourceType);
            }
        }
        return queryFields;
    }

    public String getQueryFieldsJSONString() {
        if (queryFields == null) {
            return null;
        }

        List<InterfaceQueryFieldReq> queryFieldList = Arrays.asList((InterfaceQueryFieldReq[])queryFields);
        List<JSONObject> results = queryFieldList.stream().map(queryField -> {
            JSONObject result = new JSONObject();
            result.put("field", queryField.getField());
            result.put("alias", queryField.getAlias());
            result.put("dim", queryField.getDim());
            result.put("type", queryField.getType());
            result.put("operator", queryField.getOperator());
            return result;
        }).collect(Collectors.toList());

        return CollectionUtils.isEmpty(results) ? null : JSONObject.toJSONString(results);
    }

    public String getGroupFieldsJSONString() {
        if (groupFields == null) {
            return null;
        }

        List<InterfaceGroupFieldReq> groupFieldList = Arrays.asList((InterfaceGroupFieldReq[])groupFields);
        List<JSONObject> results = groupFieldList.stream().map(groupField -> {
            JSONObject result = new JSONObject();
            result.put("field", groupField.getField());
            result.put("alias", groupField.getAlias());
            result.put("dim", groupField.getDim());
            result.put("type", groupField.getType());
            result.put("operator", groupField.getOperator());
            result.put("granularity", groupField.getGranularity());
            return result;
        }).collect(Collectors.toList());

        return CollectionUtils.isEmpty(results) ? null : JSONObject.toJSONString(results);
    }

    public String getSortFieldsJSONString() {
        if (sortFields == null) {
            return null;
        }

        List<InterfaceSortFieldReq> sortFieldList = Arrays.asList((InterfaceSortFieldReq[])sortFields);
        List<JSONObject> results = sortFieldList.stream().map(sortField -> {
            JSONObject result = new JSONObject();
            result.put("dim", sortField.getDim());
            result.put("order", sortField.getOrder());
            result.put("mode", sortField.getMode());
            result.put("format", sortField.getFormat());
            return result;
        }).collect(Collectors.toList());

        return CollectionUtils.isEmpty(results) ? null : JSONObject.toJSONString(results);
    }

    public String getRequestParamsJSONString() {
        if (requestParams == null) {
            return null;
        }

        List<InterfaceRequestParamReq> requestParamList = Arrays.asList((InterfaceRequestParamReq[])requestParams);
        List<JSONObject> results = requestParamList.stream().map(requestParam -> {
            JSONObject result = new JSONObject();
            result.put("name", requestParam.getName());
            result.put("alias", requestParam.getAlias());
            result.put("type", requestParam.getType());
            result.put("required", requestParam.getRequired());
            result.put("defaultValue", requestParam.getDefaultValue());
            return result;
        }).collect(Collectors.toList());

        return CollectionUtils.isEmpty(results) ? null : JSONObject.toJSONString(results);
    }

//    public String getResponseParamsJSONString() {
//        List<InterfaceResponseParamReq> responseParamList = Arrays.asList((InterfaceResponseParamReq[])responseParams);
//        List<JSONObject> results = responseParamList.stream().map(responseParam -> {
//            JSONObject result = new JSONObject();
//            result.put("name", responseParam.getName());
//            result.put("alias", responseParam.getAlias());
//            result.put("type", responseParam.getType());
//            return result;
//        }).collect(Collectors.toList());
//
//        return JSONObject.toJSONString(results);
//    }

    public String getResponseParamsJSONString() {
        Preconditions.checkArgument(responseParams != null , "返回参数不能为空");

        List<String> validResponseParams = new ArrayList<>();
        if (queryFields != null) {
            for (InterfaceQueryFieldReq queryField : queryFields) {
                validResponseParams.add(queryField.getField());
            }
        }
        if (groupFields != null) {
            for (InterfaceGroupFieldReq groupField : groupFields) {
                validResponseParams.add(groupField.getField());
            }
        }

        List<String> responseParamList = Arrays.asList((String[])responseParams);

        Preconditions.checkArgument(validResponseParams.containsAll(responseParamList) , "返回参数错误,需要包含在查询字段和分组字段中");
        return JSONObject.toJSONString(responseParamList);
    }
}
