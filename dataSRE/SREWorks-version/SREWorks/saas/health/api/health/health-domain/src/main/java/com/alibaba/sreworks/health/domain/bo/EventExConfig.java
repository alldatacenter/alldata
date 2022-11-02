package com.alibaba.sreworks.health.domain.bo;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.sreworks.health.domain.req.definition.DefinitionExConfigReq;
import lombok.Data;

/**
 * 事件定义扩展配置
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/05 10:58
 */
@Data
public class EventExConfig extends DefinitionExConfig {
    @JSONField(name = "type")
    protected String type;

    @JSONField(name = "storage_days")
    Integer storageDays;


    @Override
    public DefinitionExConfigReq convertToReq() {
        DefinitionExConfigReq req = new DefinitionExConfigReq();
        req.setStorageDays(storageDays);
        req.setType(type);

        return req;
    }

    public JSONObject buildRet() {
        JSONObject ret = new JSONObject();
        ret.put("storageDays", storageDays);
        ret.put("type", type);
        return ret;
    }
}
