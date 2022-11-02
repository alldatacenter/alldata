package com.alibaba.sreworks.health.domain.req.alert;

import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 告警实例请求基类
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/04 17:19
 */
@Data
@ApiModel(value = "告警实例")
public class AlertInstanceBaseReq {
    @ApiModelProperty(value = "告警定义ID", example = "1")
    Integer defId;

    @ApiModelProperty(value = "应用实例ID", example = "app")
    String appInstanceId;

    @ApiModelProperty(value = "应用组件实例ID", example = "component")
    String appComponentInstanceId;

    @ApiModelProperty(value = "指标实例ID", example = "metricInstanceId")
    String metricInstanceId;

    @ApiModelProperty(value = "实例标签", example = "xxx")
    JSONObject metricInstanceLabels;

    @ApiModelProperty(value = "发生时间戳", example = "0")
    Long occurTs;

    @ApiModelProperty(value = "告警来源", example = "xx系统")
    String source;

    @ApiModelProperty(value = "告警等级", example = "WARNING")
    String level;

    @ApiModelProperty(value = "告警接收人", example = "user1,user2")
    String receivers;

    @ApiModelProperty(value = "告警内容", example = "xxxx")
    String content;
}
