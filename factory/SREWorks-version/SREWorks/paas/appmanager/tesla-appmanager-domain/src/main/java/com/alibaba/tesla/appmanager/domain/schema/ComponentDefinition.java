package com.alibaba.tesla.appmanager.domain.schema;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

/**
 * Component Definition 对象
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Data
public class ComponentDefinition implements Schema, Serializable {

    private static final long serialVersionUID = 2803234075427830761L;

    /**
     * API 版本号
     */
    private String apiVersion;

    /**
     * 类型
     */
    private String kind;

    /**
     * 元信息
     */
    private MetaData metadata;

    /**
     * 定义描述文件
     */
    private Spec spec;

    /**
     * 元信息
     */
    @Data
    public static class MetaData implements Serializable {

        private static final long serialVersionUID = 9167909678949126254L;

        private String name;
        private Object labels;
        private Object annotations;
    }

    /**
     * Spec
     */
    @Data
    public static class Spec implements Serializable {

        private static final long serialVersionUID = -709118780118030537L;

        /**
         * 语义定义
         */
        private Semantic semantic;

        /**
         * Workload 类型映射
         */
        private WorkloadTypeDescriptor workload;
    }

    /**
     * 语义定义
     */
    @Data
    public static class Semantic implements Serializable {

        private static final long serialVersionUID = -291553600645245969L;

        /**
         * Jinja 模板
         */
        private String jinja;
    }

    /**
     * Workload 类型映射
     */
    @Data
    public static class WorkloadTypeDescriptor implements Serializable {

        private static final long serialVersionUID = 8803696482760641567L;

        /**
         * 引用类型名称
         */
        private String type;

        /**
         * Workload 引用 by GVK (和 type 字段互斥)
         */
        private WorkloadGVK definition;
    }

    @Data
    public static class WorkloadGVK implements Serializable {

        private static final long serialVersionUID = 971194211913247525L;

        /**
         * API 版本
         */
        private String apiVersion;

        /**
         * 类型
         */
        private String kind;
    }
}