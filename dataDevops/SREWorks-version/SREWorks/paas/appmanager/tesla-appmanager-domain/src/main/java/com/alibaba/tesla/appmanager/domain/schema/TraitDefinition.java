package com.alibaba.tesla.appmanager.domain.schema;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Trait 定义 class
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Data
public class TraitDefinition implements Serializable {

    private static final long serialVersionUID = 7095150105558418313L;

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
     * Trait spec
     */
    private Spec spec;

    /**
     * 元信息
     */
    @Data
    public static class MetaData implements Serializable {

        private static final long serialVersionUID = -3040036214175436386L;

        private String name;
        private String namespace;
        private JSONObject labels = new JSONObject();
        private JSONObject annotations = new JSONObject();
    }

    /**
     * Spec
     */
    @Data
    public static class Spec implements Serializable {

        private static final long serialVersionUID = -1592974598970779373L;

        private List<String> appliesToWorkloads;

        private SpecDefinitionRef definitionRef;

        private String workloadRefPath;

        private String className;

        private String runtime;

        private List<DataOutput> dataOutputs = new ArrayList<>();

        private JSONObject example = new JSONObject();
    }

    /**
     * defintionRef
     */
    @Data
    public static class SpecDefinitionRef implements Serializable {

        private static final long serialVersionUID = -6624030971546732668L;

        private String name;
    }

    @Data
    public static class DataOutput implements Serializable {

        private static final long serialVersionUID = 2150471143237594995L;

        /**
         * 产出变量名称
         */
        private String name;

        /**
         * 产出变量来源路径
         */
        private String fieldPath;

        /**
         * 应用目标 JSON Path 列表
         */
        private List<String> applyFieldPaths;
    }
}
