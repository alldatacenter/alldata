package com.alibaba.sreworks.health.domain.req.incident;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2022/01/23 15:25
 */
@Data
@ApiModel(value = "定义-应用实例-组件实例三元组")
public class DefAppComponentInstanceReq {

    @ApiModelProperty(value = "异常定义ID", example = "1")
    Integer defId;

    @ApiModelProperty(value = "应用实例ID", example = "app")
    String appInstanceId;

    @ApiModelProperty(value = "应用组件实例ID", example = "component")
    String appComponentInstanceId;
}
