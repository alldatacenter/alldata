package com.alibaba.sreworks.dataset.domain.req.model;

import com.alibaba.sreworks.dataset.common.constant.ValidConstant;
import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * 模型配置信息
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/21 17:04
 */

@Data
@ApiModel(value="数据模型配置信息")
public class DataModelConfigBaseReq {
    @ApiModelProperty(value = "模型名称", example = "CPU利用率")
    String name;

    @ApiModelProperty(value = "模型标识", example = "cpu_usage")
    String label;

//    @ApiModelProperty(value = "内置模型")
//    Boolean buildIn;

    @ApiModelProperty(value = "数据域ID", example = "1")
    Integer domainId;

    @ApiModelProperty(value = "归属团队ID", example = "0")
    Integer teamId;

    @ApiModelProperty(value = "数据源类型", example = "es")
    String sourceType;

    @ApiModelProperty(value = "数据源ID", example = "sreworks")
    String sourceId;

    @ApiModelProperty(value = "数据表", example = "metricbeat")
    String sourceTable;

    @ApiModelProperty(value = "查询条件", example = "结合数据源提供(es:lucene query)")
    String query;

    @ApiModelProperty(value = "条件域列表(es类型自带起止时间戳)")
    DataModelQueryFieldReq[] queryFields;

    @ApiModelProperty(value = "数据时间粒度", example = "1d")
    String granularity;

    @ApiModelProperty(value = "查询域列表")
    DataModelValueFieldReq[] valueFields;

    @ApiModelProperty(value = "分组域列表")
    DataModelGroupFieldReq[] groupFields;

    @ApiModelProperty(value = "说明")
    String description;

    public String getSourceType() {
        if(StringUtils.isNotEmpty(sourceType)) {
            Preconditions.checkArgument(ValidConstant.SOURCE_TYPE_LIST.contains(sourceType),
                    String.format("参数错误, 数据源[%s]不支持, 仅支持数据源[%s]", sourceType, ValidConstant.SOURCE_TYPE_LIST));
        }
        return sourceType;
    }

    public String getGranularity() {
        if(StringUtils.isNotEmpty(granularity)) {
            Preconditions.checkArgument(ValidConstant.GRANULARITY_LIST.contains(granularity),
                    String.format("参数错误, 数据粒度[%s]不支持, 仅支持数据粒度[%s]", granularity, ValidConstant.GRANULARITY_LIST));
        }
        return granularity;
    }

    public DataModelValueFieldReq[] getValueFields() {
        if (valueFields != null) {
            if(sourceType.equals(ValidConstant.ES_SOURCE)) {
                for (DataModelValueFieldReq valueField : valueFields) {
                    valueField.reqEsCheck();
                }
            } else {
                for (DataModelValueFieldReq valueField : valueFields) {
                    valueField.reqCheck();
                }
            }
        }

        return valueFields;
    }

    public DataModelGroupFieldReq[] getGroupFields() {
        if (groupFields != null) {
            if(sourceType.equals(ValidConstant.ES_SOURCE)) {
                for (DataModelGroupFieldReq groupField : groupFields) {
                    groupField.reqEsCheck();
                }
            } else {
                for (DataModelGroupFieldReq groupField : groupFields) {
                    groupField.reqCheck();
                }
            }
        }
        return groupFields;
    }

    public DataModelQueryFieldReq[] getQueryFields() {
        if (queryFields != null) {
            for (DataModelQueryFieldReq queryField : queryFields) {
                queryField.reqCheck();
            }
        }
        return queryFields;
    }
}
