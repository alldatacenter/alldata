package com.alibaba.sreworks.health.domain.req.failure;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 故障实例请求基类
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/27 14:32
 */

@Data
@ApiModel(value = "故障实例")
public class FailureInstanceBaseReq {

    @ApiModelProperty(value = "故障定义ID", example = "0")
    Integer defId;

    @ApiModelProperty(value = "应用实例ID", example = "app")
    String appInstanceId;

    @ApiModelProperty(value = "应用组件实例ID", example = "component")
    String appComponentInstanceId;

    @ApiModelProperty(value = "异常实例ID", example = "0")
    Long incidentId;

    @ApiModelProperty(value = "故障名称", example = "0")
    String name;

    @ApiModelProperty(value = "故障等级", example = "0")
    String level;

    @ApiModelProperty(value = "故障发生时间戳", example = "0")
    Long occurTs;

    @ApiModelProperty(value = "故障恢复时间戳", example = "0")
    Long recoveryTs;

    @ApiModelProperty(value = "故障详情", example = "xx")
    String content;
}
