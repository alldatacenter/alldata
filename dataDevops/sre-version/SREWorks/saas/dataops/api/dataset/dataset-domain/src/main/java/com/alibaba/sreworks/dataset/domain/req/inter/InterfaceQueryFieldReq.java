package com.alibaba.sreworks.dataset.domain.req.inter;

import com.alibaba.sreworks.dataset.common.constant.Constant;
import com.alibaba.sreworks.dataset.common.constant.ValidConstant;
import com.alibaba.sreworks.dataset.common.utils.Regex;
import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * 数据接口查询字段
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/21 17:04
 */

@Data
@ApiModel(value="数据接口查询字段")
public class InterfaceQueryFieldReq {

    @ApiModelProperty(value = "字段名称", example = "appId")
    String field;

    @ApiModelProperty(value = "字段别名", example = "应用ID")
    String alias;

    @ApiModelProperty(value = "索引字段", example = "appId")
    String dim;

    @ApiModelProperty(value = "字段类型", example = "STRING")
    String type;

    @ApiModelProperty(value = "运算函数", example = "max")
    String operator;

    public String getAlias() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(alias), "字段别名不允许为空");
        return alias;
    }

    public String getDim() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(dim), "索引字段不允许为空");
        return dim;
    }

    public String getType() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(type), "字段类型不允许为空");
        return type.toUpperCase();
    }

    public String getField() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(field), "字段名称不允许为空");
        Preconditions.checkArgument(Regex.checkDocumentByPattern(field, Constant.INTERFACE_NAME_PATTERN),
                "字段名称不符合规范:" + Constant.INTERFACE_NAME_REGEX);
        return field;
    }

    public String getOperator() {
        if (StringUtils.isNotEmpty(operator)) {
            operator = operator.toLowerCase();
            Preconditions.checkArgument(ValidConstant.ES_FIELD_AGG_TYPE_LIST.contains(operator),
                    String.format("不支持计算类型%s, 目前仅支持%s", operator, ValidConstant.ES_FIELD_AGG_TYPE_LIST));
        }

        return operator;
    }

    public void validReq(String dataSourceType) {
        getAlias();
        getField();
        getDim();
        getType();
        if (ValidConstant.ES_SOURCE.equals(dataSourceType)) {
            getOperator();
        }
    }
}
