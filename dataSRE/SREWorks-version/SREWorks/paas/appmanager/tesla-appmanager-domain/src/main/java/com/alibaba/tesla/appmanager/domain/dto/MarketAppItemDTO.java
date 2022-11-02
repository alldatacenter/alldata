package com.alibaba.tesla.appmanager.domain.dto;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Market App Item
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketAppItemDTO implements Serializable {

    private static final long serialVersionUID = -7070299131779047844L;

    /**
     * ID
     */
    private Long id;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 最后修改时间
     */
    private Date gmtModified;

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
     * Package 路径
     */
    private String packagePath;

    /**
     * Package MD5
     */
    private String packageMd5;

    /**
     * 创建者
     */
    private String packageCreator;

    /**
     * 扩展信息 JSON
     */
    private String packageExt;

    /**
     * 组件计数
     */
    private Long componentCount;

    /**
     * 简易版本号
     */
    private String simplePackageVersion;

    /**
     * 应用包 Schema 定义信息 (YAML)
     */
    private String appSchema;

    /**
     * 应用配置信息 (JSON)
     */
    private JSONObject appOptions;

    /**
     * 默认应用部署 Yaml
     */
    private String swapp;

    /**
     * Tag 列表
     */
    private List<String> tags = new ArrayList<>();

    /**
     * 当前应用的全量包计数
     */
    private Long packageCount;
}
