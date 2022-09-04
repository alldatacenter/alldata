package com.alibaba.tesla.appmanager.server.repository.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 管控单元表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UnitDO {
    /**
     * ID
     */
    private Long id;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 最后修改时间
     */
    private Date gmtModified;

    /**
     * 单元 ID (唯一标识)
     */
    private String unitId;

    /**
     * 单元名称
     */
    private String unitName;

    /**
     * 单元地址
     */
    private String endpoint;

    /**
     * 代理 IP
     */
    private String proxyIp;

    /**
     * 代理 Port
     */
    private String proxyPort;

    /**
     * Client ID
     */
    private String clientId;

    /**
     * Client Secret
     */
    private String clientSecret;

    /**
     * Username
     */
    private String username;

    /**
     * Password
     */
    private String password;

    /**
     * abm-operator Endpoint
     */
    private String operatorEndpoint;

    /**
     * Registry Endpoint
     */
    private String registryEndpoint;

    /**
     * 分类
     */
    private String category;

    /**
     * 扩展信息字段JSON
     */
    private String extra;
}