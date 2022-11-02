package com.alibaba.sreworks.health.domain.req.definition;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 事件定义模型基类
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/19 16:30
 */
@Data
@ApiModel(value = "事件定义")
public class DefinitionBaseReq {
    @ApiModelProperty(value = "定义名称", example = "CPU负载")
    String name;

    @ApiModelProperty(value = "定义分类", example = "event/risk/alert/incident")
    String category;

    @ApiModelProperty(value = "应用ID", example = "0")
    String appId;

    @ApiModelProperty(value = "应用名称", example = "APP")
    String appName;

    @ApiModelProperty(value = "应用组件名称", example = "APP_COMPONENT")
    String appComponentName;

    @ApiModelProperty(hidden = true)
    Integer metricId;

    @ApiModelProperty(hidden = true)
    String creator;

    @ApiModelProperty(value = "通知接收用户", example = "1,2,3")
    String receivers;

    @ApiModelProperty(hidden = true)
    String lastModifier;

    @ApiModelProperty(value = "扩展配置")
    DefinitionExConfigReq exConfig;

    @ApiModelProperty(value = "定义说明")
    String description;
}
