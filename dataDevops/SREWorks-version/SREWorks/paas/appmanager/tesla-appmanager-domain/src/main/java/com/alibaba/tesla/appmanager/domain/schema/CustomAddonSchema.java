package com.alibaba.tesla.appmanager.domain.schema;

import lombok.Data;

import java.io.Serializable;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.domain.core.CustomWorkloadResource;

/**
 * @ClassName:CustomAddonSchema
 * @author yangjie.dyj@alibaba-inc.com
 * @DATE: 2020-11-25
 * @Description:
 **/
@Data
public class CustomAddonSchema implements Schema, Serializable {

    private static final long serialVersionUID = 1471386093648407940L;

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
    private ComponentSchema.MetaData metadata;

    /**
     * 定义描述文件
     */
    private CustomAddonSchema.Spec spec;

    @Data
    public static class Spec implements Serializable{

        private static final long serialVersionUID = 9046093793450236498L;

        private CustomWorkloadResource workload;
    }
}
