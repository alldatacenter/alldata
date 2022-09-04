package com.alibaba.tesla.appmanager.domain.dto;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.domain.schema.AppPackageSchema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Market App Item
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketPackageDTO implements Serializable {

    /**
     * 应用唯一标识
     */
    private String appId;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 版本号
     */
    private String packageVersion;

    /**
     * 短版本号
     */
    private String simplePackageVersion;

    /**
     * Package 本地路径
     */
    private String packageLocalPath;

    /**
     * 应用配置信息 (JSON)
     */
    private JSONObject appOptions;

    /**
     * 应用包 Schema 定义信息 (YAML)
     */
    private AppPackageSchema appSchemaObject;

}
