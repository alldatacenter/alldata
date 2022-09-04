package com.alibaba.tesla.appmanager.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 实时应用实例 DTO
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RtAppInstanceDTO {

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 修改时间
     */
    private Date gmtModified;

    /**
     * 应用实例 ID
     */
    private String appInstanceId;

    /**
     * 应用实例名称
     */
    private String appInstanceName;

    /**
     * 应用 ID
     */
    private String appId;

    /**
     * Cluster ID
     */
    private String clusterId;

    /**
     * 应用实例版本号 (最近一次应用包部署)
     */
    private String version;

    /**
     * 简化 Version
     */
    private String simpleVersion;

    /**
     * 状态
     */
    private String status;

    /**
     * 是否可访问
     */
    private Boolean visit;

    /**
     * 是否可升级
     */
    private Boolean upgrade;

    /**
     * 应用实例可升级最新版本号
     */
    private String latestVersion;

    /**
     * 应用实例可升级最新版本号（简化版本）
     */
    private String latestSimpleVersion;

    /**
     * Namespace ID
     */
    private String namespaceId;

    /**
     * Stage ID
     */
    private String stageId;

    /**
     * 包含的组件实例状态列表
     */
    private List<RtComponentInstanceDTO> components = new ArrayList<>();

    /**
     * 应用选项
     */
    private Options options;

    /**
     * 应用信息
     */
    @Data
    @Builder
    public static class Options {

        /**
         * 应用名称
         */
        private String name;

        /**
         * 应用描述
         */
        private String description;

        /**
         * 分类
         */
        private String category;

        /**
         * Logo
         */
        private String logoImg;

        /**
         * 中文名称
         */
        private String nameCn;

        /**
         * 导航链接
         */
        private String navLink;
    }
}
