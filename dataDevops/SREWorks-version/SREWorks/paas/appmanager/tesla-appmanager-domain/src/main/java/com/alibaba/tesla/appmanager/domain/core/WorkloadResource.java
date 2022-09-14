package com.alibaba.tesla.appmanager.domain.core;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Workload Resource 对象
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Data
public class WorkloadResource implements Serializable {

    private static final long serialVersionUID = 133506853433623661L;

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
    private Object spec = new JSONObject();

    /**
     * 数据输出列表
     */
    private List<DataOutput> dataOutputs = new ArrayList<>();

    /**
     * 元信息
     */
    @Data
    public static class MetaData implements Serializable {

        private static final long serialVersionUID = -7986607001492546572L;

        private String namespace;
        private String name;
        private Object labels = new JSONObject();
        private Object annotations = new JSONObject();
    }

    @Data
    public static class DataOutput implements Serializable {

        private static final long serialVersionUID = 3957915292551325502L;

        /**
         * 产出变量名称
         */
        private String name;

        /**
         * 产出变量来源路径
         */
        private String fieldPath;
    }
}
