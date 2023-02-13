package com.alibaba.tesla.appmanager.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddonInstanceComponentRelExtDTO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * create time
     */
    private Date gmtCreate;

    /**
     * modify time
     */
    private Date gmtModified;

    /**
     * namespace id
     */
    private String namespaceId;

    /**
     * env id
     */
    private String envId;

    /**
     * app id
     */
    private String appId;

    /**
     * componentType
     */
    private String componentType;

    /**
     * component name
     */
    private String componentName;

    /**
     * addon instanceId
     */
    private String addonInstanceId;

    /**
     * 全局变量
     */
    private Map<String, Object> globalVars;

    /**
     * addonName
     */
    private String addonName;

    /**
     * addon Id
     */
    private String addonId;

    /**
     * addon version
     */
    private String addonVersion;
}
