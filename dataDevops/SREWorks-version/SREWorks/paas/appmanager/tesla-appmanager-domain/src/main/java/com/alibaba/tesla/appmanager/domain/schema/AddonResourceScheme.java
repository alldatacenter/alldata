package com.alibaba.tesla.appmanager.domain.schema;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Data
public class AddonResourceScheme implements Schema, Serializable {

    private static final long serialVersionUID = -2415106305621623316L;

    private AddonResourceDescribe addon;

    @Data
    public static class AddonResourceDescribe implements Serializable {

        /**
         * addon scheme
         */
        private String version;

        /**
         * 资源描述
         */
        private List<AddonResource> resources;
    }

    @Data
    public static class AddonResource implements Serializable {

        /**
         * addon id
         */
        private String addonId;

        /**
         * addon 版本
         */
        private String addonVersion;

        /**
         * 资源别名
         */
        private String name;

        /**
         * params
         */
        private Map<String, Object> params;

        /**
         * mapping
         */
        private Map<String, String> mapping;
    }
}
