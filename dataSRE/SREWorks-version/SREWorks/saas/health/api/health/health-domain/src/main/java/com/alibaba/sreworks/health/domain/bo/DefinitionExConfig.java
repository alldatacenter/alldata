package com.alibaba.sreworks.health.domain.bo;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.health.domain.req.definition.DefinitionExConfigReq;

/**
 * 事件定义个性化配置
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/19 16:30
 */
public abstract class DefinitionExConfig {

    public abstract DefinitionExConfigReq convertToReq();

    public abstract JSONObject buildRet();
}
