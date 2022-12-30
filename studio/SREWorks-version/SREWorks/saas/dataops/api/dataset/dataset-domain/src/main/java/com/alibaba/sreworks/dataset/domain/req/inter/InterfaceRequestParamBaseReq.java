package com.alibaba.sreworks.dataset.domain.req.inter;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 数据接口请求参数
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/21 17:04
 */

@Data
@ApiModel(value="数据接口请求参数")
public class InterfaceRequestParamBaseReq {

    @ApiModelProperty(value = "参数名称", example = "appId")
    String name;

    @ApiModelProperty(value = "参数别名", example = "应用ID")
    String alias;

    @ApiModelProperty(value = "参数类型", example = "STRING")
    String type;

    @ApiModelProperty(value = "必填", example = "true")
    Boolean required;

    @ApiModelProperty(value = "默认值", example = "xx")
    String defaultValue;
}
