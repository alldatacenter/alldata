package com.alibaba.sreworks.dataset.domain.req.inter;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 数据接口查询字段
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/21 17:04
 */

@Data
@ApiModel(value="数据接口查询字段")
public class InterfaceQueryFieldBaseReq {

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
}
