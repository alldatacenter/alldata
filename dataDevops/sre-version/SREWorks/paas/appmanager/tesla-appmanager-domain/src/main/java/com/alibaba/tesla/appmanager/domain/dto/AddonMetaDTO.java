package com.alibaba.tesla.appmanager.domain.dto;

import java.util.Date;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddonMetaDTO {
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
     * 类型（可选 INTERNAL / RESOURCE）
     */
    private String addonType;

    /**
     * 附加组件唯一标识
     */
    private String addonId;

    /**
     * 版本号
     */
    private String addonVersion;

    /**
     * 存储相对路径
     */
    private String addonLabel;

    /**
     * 附加组件描述
     */
    private String addonDescription;

    /**
     * 附加组件定义 Schema
     */
    private String addonSchema;

    /**
     * 附件组件配置Schema
     */
    private JSONObject componentsSchema;
}
