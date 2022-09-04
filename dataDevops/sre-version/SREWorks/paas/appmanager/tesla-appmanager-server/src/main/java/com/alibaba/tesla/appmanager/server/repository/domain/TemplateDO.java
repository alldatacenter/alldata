package com.alibaba.tesla.appmanager.server.repository.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 应用模板
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TemplateDO implements Serializable {
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
     * 模板全局唯一 ID
     */
    private String templateId;

    /**
     * 模板版本
     */
    private String templateVersion;

    /**
     * 模板类型
     */
    private String templateType;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 存储位置相对路径
     */
    private String templatePath;

    /**
     * 模板扩展信息
     */
    private String templateExt;

    private static final long serialVersionUID = 1L;
}