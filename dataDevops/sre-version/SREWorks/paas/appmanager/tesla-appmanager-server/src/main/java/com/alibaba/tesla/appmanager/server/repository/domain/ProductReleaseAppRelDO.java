package com.alibaba.tesla.appmanager.server.repository.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 产品版本映射应用表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductReleaseAppRelDO {
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
     * 产品 ID
     */
    private String productId;

    /**
     * 发布版本 ID
     */
    private String releaseId;

    /**
     * 应用 ID
     */
    private String appId;

    /**
     * 覆盖默认 tag (用于定制场景)
     */
    private String tag;

    /**
     * 基线 Git 分支
     */
    private String baselineGitBranch;

    /**
     * build.yaml 相对路径
     */
    private String baselineBuildPath;

    /**
     * launch.yaml 相对路径
     */
    private String baselineLaunchPath;
}