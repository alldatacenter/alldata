package com.alibaba.sreworks.health.domain.req.risk;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 风险实例请求基类
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/05 16:27
 */
@Data
@ApiModel(value = "风险实例")
public class RiskInstanceBaseReq {

    @ApiModelProperty(value = "风险定义ID", example = "1")
    Integer defId;

    @ApiModelProperty(value = "应用实例ID", example = "app")
    String appInstanceId;

    @ApiModelProperty(value = "应用组件实例ID", example = "component")
    String appComponentInstanceId;

    @ApiModelProperty(value = "发生时间戳", example = "1635391617000")
    Long occurTs;

    @ApiModelProperty(value = "风险来源", example = "yyy")
    String source;

    @ApiModelProperty(value = "风险详情", example = "xxxxx")
    String content;
}
