package com.alibaba.tesla.appmanager.server.addon.req;

import com.alibaba.tesla.appmanager.domain.schema.ComponentSchema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckAddonInstanceExpiredReq implements Serializable {

    private static final long serialVersionUID = 2260916277805399297L;

    /**
     * 命名空间 ID
     */
    private String namespaceId;

    /**
     * Addon ID
     */
    private String addonId;

    /**
     * Addon Name
     */
    private String addonName;

    /**
     * Addon 属性字典
     */
    private Map<String, String> addonAttrs;

    /**
     * 上一次 Addon Schema
     */
    private ComponentSchema lastSchema;

    /**
     * 上一次 Addon Signature
     */
    private String lastSignature;

    /**
     * 当前 Addon Schema
     */
    private ComponentSchema currentSchema;
}
