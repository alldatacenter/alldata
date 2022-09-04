package com.alibaba.sreworks.health.domain.req.incident;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 异常实例请求基类
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/27 14:32
 */

@Data
@ApiModel(value = "异常实例")
public class IncidentInstanceBaseReq {

    @ApiModelProperty(value = "异常定义ID", example = "1")
    Integer defId;

    @ApiModelProperty(value = "应用实例ID", example = "app")
    String appInstanceId;

    @ApiModelProperty(value = "应用组件实例ID", example = "component")
    String appComponentInstanceId;

    @ApiModelProperty(value = "开始发生时间戳", example = "1635391617000")
    Long occurTs;

    @ApiModelProperty(value = "恢复时间戳", example = "1635391617000")
    Long recoveryTs;

    @ApiModelProperty(value = "异常事件来源", example = "巡检/异常检测")
    String source;

    @ApiModelProperty(value = "异常产生原因")
    String cause;

    @ApiModelProperty(value = "任务链ID", example = "0ad1348f1403169275002100356696")
    String traceId;

    @ApiModelProperty(value = "任务链层次ID", example = "0")
    String spanId;

    @ApiModelProperty(value = "自愈开始时间戳", example = "1635391617000")
    Long selfHealingStartTs;

    @ApiModelProperty(value = "自愈结束时间戳", example = "1635391617000")
    Long selfHealingEndTs;

    @ApiModelProperty(value = "自愈状态", example = "running")
    String selfHealingStatus;

    @ApiModelProperty(value = "扩展信息(包括用户自定义参数)", example = "")
    String options;

    @ApiModelProperty(value = "实例说明")
    String description;
}
