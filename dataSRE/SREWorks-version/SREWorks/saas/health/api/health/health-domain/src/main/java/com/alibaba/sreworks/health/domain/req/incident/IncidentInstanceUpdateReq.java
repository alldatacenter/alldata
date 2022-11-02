package com.alibaba.sreworks.health.domain.req.incident;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 更新异常实例请求
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/27 14:32
 */
@Data
@ApiModel(value = "更新异常实例")
public class IncidentInstanceUpdateReq {

    @ApiModelProperty(value = "异常定义ID", example = "1")
    Integer defId;

    @ApiModelProperty(value = "应用实例ID", example = "app")
    String appInstanceId;

    @ApiModelProperty(value = "应用组件实例ID", example = "component")
    String appComponentInstanceId;

    @ApiModelProperty(value = "最近发生时间戳", example = "1635391617000")
    Long lastOccurTs;

    @ApiModelProperty(value = "异常事件来源", example = "巡检/异常检测")
    String source;

    @ApiModelProperty(value = "异常产生原因")
    String cause;

    @ApiModelProperty(value = "扩展信息(包括用户自定义参数)", example = "")
    String options;

    @ApiModelProperty(value = "实例说明")
    String description;
}
