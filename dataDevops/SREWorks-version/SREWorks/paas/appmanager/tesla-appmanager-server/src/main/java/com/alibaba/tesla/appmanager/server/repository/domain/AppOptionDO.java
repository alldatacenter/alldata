package com.alibaba.tesla.appmanager.server.repository.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 应用配置选项表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AppOptionDO {
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
     * 应用 ID
     */
    private String appId;

    /**
     * Key
     */
    private String key;

    /**
     * Value
     */
    private String value;

    /**
     * Value 类型
     */
    private String valueType;
}