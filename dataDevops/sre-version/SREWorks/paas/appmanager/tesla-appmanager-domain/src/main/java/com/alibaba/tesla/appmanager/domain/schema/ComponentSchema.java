package com.alibaba.tesla.appmanager.domain.schema;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.domain.core.ImageTar;
import com.alibaba.tesla.appmanager.domain.core.WorkloadResource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Component Schema 对象
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Data
public class ComponentSchema implements Schema, Serializable {

    private static final long serialVersionUID = -7797463529846059161L;

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

        private static final long serialVersionUID = -7986607001492546572L;

        private String name;
        private Object labels;
        private Object annotations;
    }

    /**
     * spec
     */
    @Data
    public static class Spec implements Serializable {

        private static final long serialVersionUID = 254059068778729289L;

        /**
         * 引用 workload
         */
        private WorkloadResource workload = new WorkloadResource();

        /**
         * 镜像存储地址
         */
        private List<ImageTar> images = new ArrayList<>();
    }

    /**
     * 覆写一些增量 vars 到 workload spec 中
     *
     * @param parameters 参数 Map
     */
    public void overwriteWorkloadSpecVars(Map<String, Object> parameters) {
        ((JSONObject) spec.workload.getSpec()).putAll(parameters);
    }
}
