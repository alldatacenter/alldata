package com.alibaba.tesla.appmanager.domain.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 应用包 Schema
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppPackageSchema implements Schema, Serializable {

    /**
     * 应用唯一标识
     */
    private String appId;

    /**
     * 版本号
     */
    private String packageVersion;

    /**
     * 创建者
     */
    private String packageCreator;

    /**
     * 当前应用包包含的组件包列表
     */
    private List<ComponentPackageItem> componentPackages = new ArrayList<>();

    /**
     * 标签列表
     */
    private List<String> tags = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComponentPackageItem implements Serializable {

        /**
         * 组件类型
         */
        private String componentType;

        /**
         * 组件类型下的唯一组件标识
         */
        private String componentName;

        /**
         * 版本号
         */
        private String packageVersion;

        /**
         * 存储位置相对路径 (相对当前包的位置)
         */
        private String relativePath;

        /**
         * 创建者
         */
        private String packageCreator;

        /**
         * 包 MD5
         */
        private String packageMd5;

        /**
         * 包 Addon 描述信息
         */
        private String packageAddon;

        /**
         * 包配置选项信息
         */
        private String packageOptions;

        /**
         * 扩展信息 JSON
         */
        private String packageExt;
    }
}
