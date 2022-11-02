package com.alibaba.tesla.appmanager.domain.schema;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

/**
 * Trait Schema 结构
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Data
public class TraitSchema implements Schema, Serializable {

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
     * Trait Spec
     */
    private Object spec = new JSONObject();

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
}
