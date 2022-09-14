package com.alibaba.sreworks.health.domain.bo;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.sreworks.health.domain.req.definition.DefinitionExConfigReq;
import lombok.Data;

/**
 * 风险定义扩展配置
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/05 10:58
 */
@Data
public class RiskExConfig extends DefinitionExConfig {
    @JSONField(name = "tags")
    protected String[] tags;

    @JSONField(name = "storage_days")
    Integer storageDays;

    @JSONField(name = "enable")
    Boolean enable;

    @JSONField(name = "weight")
    Integer weight;

    @Override
    public DefinitionExConfigReq convertToReq() {
        DefinitionExConfigReq req = new DefinitionExConfigReq();
        req.setEnable(enable);
        req.setStorageDays(storageDays);
        req.setTags(tags);
        req.setWeight(weight);
        return req;
    }

    public JSONObject buildRet() {
        JSONObject ret = new JSONObject();
        ret.put("enable", enable);
        ret.put("tags", tags);
        ret.put("storageDays", storageDays);
        ret.put("weight", weight);
        return ret;
    }
}
