package com.alibaba.sreworks.dataset.domain.req.inter;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 数据接口参数信息
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/21 17:04
 */

@Data
@ApiModel(value="数据接口参数信息")
public class DataInterfaceParamBaseReq {
    @ApiModelProperty(value = "参数标识", example = "appId")
    String label;

    @ApiModelProperty(value = "参数名称", example = "应用ID")
    String name;

    @ApiModelProperty(value = "归属接口ID(新建接口不需要提供)", example = "1")
    Integer interfaceId;

    @ApiModelProperty(value = "参数类型", example = "STRING")
    String type;

    @ApiModelProperty(value = "必填", example = "true")
    Boolean required;

    @ApiModelProperty(value = "默认值", example = "xx")
    String defaultValue;
}
