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
@ApiModel(value="数据模型分组信息")
public class DataModelGroupFieldReq{
    @ApiModelProperty(value = "分组方式", example = "terms")
    String operator;

    @ApiModelProperty(value = "源字段标识", example = "kubernetes.labels.appId")
    String dim;

    @ApiModelProperty(value = "字段类型", example = "STRING")
    String type;

    @ApiModelProperty(value = "模型字段标识", example = "appId")
    String field;

    @ApiModelProperty(value = "字段描述", example = "description")
    String description;

    /**
     * ES数据源 模型分组字段对象合法性校验 分组仅支持 date_histogram terms两种
     */
    public void reqEsCheck() {
        reqCheck();

        Preconditions.checkArgument(StringUtils.isNotEmpty(operator) , "数据模型分组字段配置错误, 聚合方式不允许为空");

        Preconditions.checkArgument(ValidConstant.ES_BUCKET_AGG_TYPE_LIST.contains(operator) , String.format("不支持聚合类型%s", operator));
    }

    public void reqCheck() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(dim) && StringUtils.isNotEmpty(type) && StringUtils.isNotEmpty(field),
                "数据模型分组字段配置错误，字段标识/字段内容/字段类型均不允许为空");
    }

    public String getType() {
        if (StringUtils.isNotEmpty(type)) {
            type = type.toUpperCase();
        }
        return type;
    }
}
