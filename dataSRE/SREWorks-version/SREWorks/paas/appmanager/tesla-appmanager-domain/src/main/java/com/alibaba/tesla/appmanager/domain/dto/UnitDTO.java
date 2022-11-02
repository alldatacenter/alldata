package com.alibaba.tesla.appmanager.domain.dto;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 单元 DTO
 *
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnitDTO implements Serializable {

    private static final long serialVersionUID = 7467435499657845401L;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 修改时间
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
     * Username
     */
    private String username;

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
     * 扩展数据 JSON
     */
    private JSONObject extra;
}
