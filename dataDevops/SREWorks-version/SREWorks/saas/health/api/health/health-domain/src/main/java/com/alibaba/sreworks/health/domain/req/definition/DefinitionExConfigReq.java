package com.alibaba.sreworks.health.domain.req.definition;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.sreworks.health.common.alert.AlertLevel;
import com.alibaba.sreworks.health.common.alert.NoticeChannel;
import com.alibaba.sreworks.health.domain.bo.AlertRuleConfig;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Map;

/**
 * 事件定义个性化配置基类
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/19 16:30
 */
@Data
@ApiModel(value = "事件定义个性化配置")
public class DefinitionExConfigReq {
    @ApiModelProperty(value = "类型ID(incident)", example = "0")
    @JSONField(name = "type_id")
    Integer typeId;

    @ApiModelProperty(value = "是否自愈(incident)", example = "false")
    @JSONField(name = "self_healing")
    Boolean selfHealing;

    @ApiModelProperty(value = "实例存储时长(alert/risk/event)", example = "7")
    @JSONField(name = "storage_days")
    Integer storageDays;

    @ApiModelProperty(value = "是否启用(alert/risk)", example = "false")
    @JSONField(name = "enable")
    Boolean enable;

//    @ApiModelProperty(value = "风险等级(risk)", example = "high")
//    @JSONField(name = "level")
//    String level;

    @ApiModelProperty(value = "标签(risk)", example = "[\"a\",\"b\"]")
    @JSONField(name = "tags")
    String[] tags;

    @ApiModelProperty(value = "事件类型(event)", example = "xxx")
    @JSONField(name = "type")
    String type;

    @ApiModelProperty(value = "健康权重(risk/alert/incident)", example = "1-10")
    @JSONField(name = "weight")
    Integer weight;

    @ApiModelProperty(value = "关联指标ID(alert)", example = "0")
    @JSONField(name = "metric_id")
    Integer metricId;

    @ApiModelProperty(value = "关联指标时间粒度(alert)", example = "1(分钟)")
    @JSONField(name = "granularity")
    Integer granularity;

    @ApiModelProperty(value = "告警规则(alert)")
    @JSONField(name = "alert_rule_config")
    AlertRuleConfig alertRuleConfig;

    @ApiModelProperty(value = "通知规则(alert)")
    @JSONField(name = "notice_config")
    Map<AlertLevel, NoticeChannel[]> noticeConfig;

    @ApiModelProperty(value = "关联异常定义ID(failure)", example = "7")
    @JSONField(name = "ref_incident_def_id")
    Integer refIncidentDefId;

    @ApiModelProperty(value = "故障等级规则(failure)", example = "{\"P4\": \"30m\", \"P3\": \"60m\", \"P2\": \"120m\", \"P1\": \"360m\", \"P0\": \"1d\"}")
    @JSONField(name = "failure_level_rule")
    JSONObject failureLevelRule;
}
