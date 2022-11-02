package com.alibaba.sreworks.dataset.domain.req.model;

import com.alibaba.sreworks.dataset.common.constant.ValidConstant;
import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * 模型列信息
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/21 17:04
 */

@Data
@ApiModel(value="数据模型字段信息")
public class DataModelValueFieldReq {
    @ApiModelProperty(value = "运算方式", example = "max")
    String operator;

    @ApiModelProperty(value = "源字段标识", example = "kubernetes.pod.cpu.usage.node.pct")
    String dim;

    @ApiModelProperty(value = "字段类型", example = "DOUBLE")
    String type;

    @ApiModelProperty(value = "模型字段标识", example = "usagePct")
    String field;

    @ApiModelProperty(value = "字段描述", example = "description")
    String description;

    /**
     * ES数据源 模型数值字段对象合法性校验
     */
    public void reqEsCheck() {
        reqCheck();

        if (StringUtils.isNotEmpty(operator)) {
            Preconditions.checkArgument(ValidConstant.ES_FIELD_AGG_TYPE_LIST.contains(operator),
                    String.format("不支持计算类型%s, 目前仅支持%s", operator, ValidConstant.ES_FIELD_AGG_TYPE_LIST));
        }
    }

    public void reqCheck() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(dim) && StringUtils.isNotEmpty(type) && StringUtils.isNotEmpty(field),
                "数据模型数值字段配置错误, 字段标识/字段内容/字段类型均不允许为空");
    }

    public String getType() {
        if (StringUtils.isNotEmpty(type)) {
            type = type.toUpperCase();
        }
        return type;
    }
}
