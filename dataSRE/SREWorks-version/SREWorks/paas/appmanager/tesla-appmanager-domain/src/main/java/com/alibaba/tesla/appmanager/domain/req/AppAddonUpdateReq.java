package com.alibaba.tesla.appmanager.domain.req;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

/**
 * 应用 Addon 绑定更新请求
 *
 * @author qianmo.zm@alibaba-inc.com
 */
@Data
public class AppAddonUpdateReq {

    /**
     * 应用 ID
     */
    private String appId;

    /**
     * Namespace ID
     */
    private String namespaceId;

    /**
     * Stage ID
     */
    private String stageId;

    /**
     * Addon Name
     */
    private String addonName;

    /**
     * Addon 配置信息
     */
    private JSONObject spec;
}
