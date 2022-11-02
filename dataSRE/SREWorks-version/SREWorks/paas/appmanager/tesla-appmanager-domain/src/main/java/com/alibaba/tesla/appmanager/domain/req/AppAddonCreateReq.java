package com.alibaba.tesla.appmanager.domain.req;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 应用 Addon 绑定新建请求
 *
 * @author qianmo.zm@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppAddonCreateReq {

    /**
     * 应用 ID
     */
    private String appId;

    /**
     * Addon 元数据 ID
     */
    private Long addonMetaId;

    /**
     * Addon Type
     */
    private String addonType;

    /**
     * Addon Id
     */
    private String addonId;

    /**
     * Addon Name
     */
    private String addonName;

    /**
     * Namespace ID
     */
    private String namespaceId;

    /**
     * Stage ID
     */
    private String stageId;

    /**
     * Addon 配置信息
     */
    private JSONObject spec;
}
