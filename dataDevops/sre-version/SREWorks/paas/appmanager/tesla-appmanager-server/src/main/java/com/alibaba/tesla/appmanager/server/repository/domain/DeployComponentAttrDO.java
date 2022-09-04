package com.alibaba.tesla.appmanager.server.repository.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 部署工单 - Component 属性存储表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeployComponentAttrDO implements Serializable {
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
     * 所属部署 Component 单 ID
     */
    private Long deployComponentId;

    /**
     * 类型
     */
    private String attrType;

    /**
     * 值
     */
    private String attrValue;

    private static final long serialVersionUID = 1L;
}