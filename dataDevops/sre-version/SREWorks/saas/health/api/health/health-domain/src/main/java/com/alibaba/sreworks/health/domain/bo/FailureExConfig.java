package com.alibaba.sreworks.health.domain.bo;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.sreworks.health.domain.req.definition.DefinitionExConfigReq;
import lombok.Data;

/**
 * 故障扩展配置
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/04 16:20
 */
@Data
public class FailureExConfig extends DefinitionExConfig {
    @JSONField(name = "ref_incident_def_id")
    protected Integer refIncidentDefId;

    @JSONField(name = "failure_level_rule")
    protected JSONObject failureLevelRule;

    @Override
    public DefinitionExConfigReq convertToReq() {
        DefinitionExConfigReq req = new DefinitionExConfigReq();
        req.setRefIncidentDefId(refIncidentDefId);
        req.setFailureLevelRule(failureLevelRule);

        return req;
    }

    public JSONObject buildRet() {
        JSONObject ret = new JSONObject();
        ret.put("refIncidentDefId", refIncidentDefId);
        ret.put("failureLevelRule", failureLevelRule);
        return ret;
    }
}
