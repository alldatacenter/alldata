package com.alibaba.sreworks.health.domain.bo;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.sreworks.health.common.alert.AlertLevel;
import com.alibaba.sreworks.health.common.alert.NoticeChannel;
import com.alibaba.sreworks.health.domain.req.definition.DefinitionExConfigReq;
import lombok.Data;

import java.util.Map;

/**
 * 告警定义个性化配置
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/19 16:30
 */

@Data
public class AlertExConfig extends DefinitionExConfig {

    @JSONField(name = "metric_id")
    Integer metricId;

    @JSONField(name = "storage_days")
    Integer storageDays;

    @JSONField(name = "enable")
    Boolean enable;

    @JSONField(name = "weight")
    Integer weight;

    @JSONField(name = "granularity")
    Integer granularity;

    @JSONField(name = "alert_rule_config")
    AlertRuleConfig alertRuleConfig;

    @JSONField(name = "notice_config")
    Map<AlertLevel, NoticeChannel[]> noticeConfig;

    @Override
    public DefinitionExConfigReq convertToReq() {
        DefinitionExConfigReq req = new DefinitionExConfigReq();
        req.setEnable(enable);
        req.setStorageDays(storageDays);
        req.setGranularity(granularity);
        req.setAlertRuleConfig(alertRuleConfig);
        req.setNoticeConfig(noticeConfig);
        req.setWeight(weight);
        return req;
    }

    public JSONObject buildRet() {
        JSONObject ret = new JSONObject();
        ret.put("enable", enable);
        ret.put("storageDays", storageDays);
//        ret.put("metricId", metricId);
        ret.put("granularity", granularity);
        ret.put("alertRuleConfig", alertRuleConfig);
        ret.put("noticeConfig", noticeConfig);
        ret.put("weight", weight);
        return ret;
    }
}
