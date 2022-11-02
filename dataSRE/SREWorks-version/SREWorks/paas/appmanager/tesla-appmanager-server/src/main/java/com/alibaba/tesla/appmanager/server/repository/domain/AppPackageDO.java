package com.alibaba.tesla.appmanager.server.repository.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 应用包详情表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AppPackageDO {
    private static final long serialVersionUID = 1L;
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
     * 应用唯一标识
     */
    private String appId;

    /**
     * 版本号
     */
    private String packageVersion;

    /**
     * 存储位置相对路径
     */
    private String packagePath;

    /**
     * 创建者
     */
    private String packageCreator;

    /**
     * 包 MD5
     */
    private String packageMd5;

    /**
     * 组件包计数
     */
    private Long componentCount;

    /**
     * VERSION
     */
    private Integer version;

    /**
     * 应用包 Schema 定义信息 (YAML)
     */
    private String appSchema;

    /**
     * 默认应用部署 YAML
     */
    private String swapp;

    /**
     * 标签列表
     */
    private List<String> tags = new ArrayList<>();
}