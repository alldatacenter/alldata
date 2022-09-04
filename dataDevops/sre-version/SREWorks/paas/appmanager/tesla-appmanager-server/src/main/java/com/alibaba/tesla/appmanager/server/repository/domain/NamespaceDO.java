package com.alibaba.tesla.appmanager.server.repository.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 命名空间
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NamespaceDO implements Serializable {
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
     * Namespace ID
     */
    private String namespaceId;

    /**
     * Namespace Name
     */
    private String namespaceName;

    /**
     * Namespace 创建者
     */
    private String namespaceCreator;

    /**
     * Namespace 最后修改者
     */
    private String namespaceModifier;

    /**
     * Namespace 扩展信息
     */
    private String namespaceExt;

    private static final long serialVersionUID = 1L;
}