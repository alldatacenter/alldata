package com.alibaba.tesla.appmanager.server.addon.req;

import com.alibaba.tesla.appmanager.domain.schema.CustomAddonSchema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * @ClassName: ApplyCustomAddonInstanceReq
 * @Author: dyj
 * @DATE: 2020-12-23
 * @Description:
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyCustomAddonInstanceReq implements Serializable {
    private static final long serialVersionUID = 2637914125447355377L;
    /**
     * 命名空间 ID
     */
    private String namespaceId;

    /**
     * Addon ID
     */
    private String addonId;

    /**
     * Creator
     */
    private String creator;

    /**
     * Addon Version
     */
    private String addonVersion;

    /**
     * Addon Name
     */
    private String addonName;

    /**
     * Addon 属性字典
     */
    private Map<String, String> addonAttrs;

    /**
     * Custom Addon Schema
     */
    private CustomAddonSchema customAddonSchema;
}
