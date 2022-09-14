package com.alibaba.sreworks.dataset.domain.req.model;

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
@ApiModel(value="数据模型条件信息")
public class DataModelQueryFieldReq {
    @ApiModelProperty(value = "字段类型", example = "STRING")
    String type;

    @ApiModelProperty(value = "模型字段标识", example = "appId")
    String field;

    @ApiModelProperty(value = "字段描述", example = "description")
    String description;

    public void reqCheck() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(type) && StringUtils.isNotEmpty(field),
                "数据模型查询字段配置错误， 字段标识/字段类型均不允许为空");
    }

    public String getType() {
        if (StringUtils.isNotEmpty(type)) {
            type = type.toUpperCase();
        }
        return type;
    }
}
