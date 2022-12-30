package com.alibaba.tesla.appmanager.server.repository.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 环境表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EnvDO implements Serializable {
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
     * 所属 Namespace ID
     */
    private String namespaceId;

    /**
     * 环境 ID
     */
    private String envId;

    /**
     * 环境名称
     */
    private String envName;

    /**
     * 环境创建者
     */
    private String envCreator;

    /**
     * 环境最后修改者
     */
    private String envModifier;

    /**
     * 环境扩展信息
     */
    private String envExt;

    private static final long serialVersionUID = 1L;
}