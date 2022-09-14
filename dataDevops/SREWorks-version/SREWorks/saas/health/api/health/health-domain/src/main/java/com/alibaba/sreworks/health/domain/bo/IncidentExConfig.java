package com.alibaba.sreworks.health.domain.bo;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.sreworks.health.domain.req.definition.DefinitionExConfigReq;
import lombok.Data;

/**
 * 异常定义个性化配置
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/19 16:30
 */

@Data
public class IncidentExConfig extends DefinitionExConfig {
    @JSONField(name = "type_id")
    protected Integer typeId;

    @JSONField(name = "self_healing")
    Boolean selfHealing;

    @JSONField(name = "weight")
    Integer weight;

    @Override
    public DefinitionExConfigReq convertToReq() {
        DefinitionExConfigReq req = new DefinitionExConfigReq();
        req.setTypeId(typeId);
        req.setSelfHealing(selfHealing);
        req.setWeight(weight);
        return req;
    }

    public JSONObject buildRet() {
        JSONObject ret = new JSONObject();
        ret.put("typeId", typeId);
        ret.put("selfHealing", selfHealing);
        ret.put("weight", weight);
        return ret;
    }
}
