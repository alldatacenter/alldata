package com.alibaba.tesla.appmanager.domain.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * AppPackage 的 Meta 信息类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppPackageMetaSchema {

    /**
     * 应用 ID
     */
    private String appId;

    /**
     * 版本号
     */
    private String version;

    /**
     * 创建人
     */
    private String creator;

    /**
     * 当前 App Package 包含的组件列表
     */
    private List<Component> components;

    /**
     * AppPackage Meta 信息的 Component 对象
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Component {

        /**
         * 组件类型
         */
        private String type;

        /**
         * 组件名称 (标识)
         */
        private String name;

        /**
         * 组件包存储相对 AppPackage 的路径
         */
        private String path;

        /**
         * 组件对应版本号
         */
        private String version;

        /**
         * 包 md5 校验
         */
        private String md5;

        /**
         * 包扩展选项
         */
        private ComponentOptions options;

        /**
         * Addon 描述
         */
        private ComponentAddon addon;
    }

    /**
     * Component 中的 options 对象，针对 microservice 类型
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComponentOptions {

        /**
         * 分支
         */
        private String branch;

        /**
         * git 仓库地址 (http 形式)
         */
        private String gitRepo;

        /**
         * 语言
         */
        private String language;

        /**
         * 服务类型
         */
        private String serviceType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComponentAddon {

        /**
         * Addon Schema 版本
         */
        private String version;

        /**
         * 资源描述
         */
        private List<ComponentAddonResource> resources;
    }

    /**
     * Component Addon 资源声明
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComponentAddonResource {

        /**
         * Addon ID
         */
        private String addonId;

        /**
         * Addon 版本
         */
        private String addonVersion;

        /**
         * Addon 参数
         */
        private Map<String, Object> params;

        /**
         * Addon ConfigVar 映射关系
         */
        private Map<String, Object> mapping;
    }
}
