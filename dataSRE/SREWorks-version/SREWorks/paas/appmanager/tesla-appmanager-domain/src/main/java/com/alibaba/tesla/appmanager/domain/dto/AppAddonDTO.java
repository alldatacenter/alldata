package com.alibaba.tesla.appmanager.domain.dto;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 应用 Addon 绑定关系 DTO
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppAddonDTO {
    /**
     * 主键
     */
    private Long id;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 修改时间
     */
    private Date gmtModified;

    /**
     * 应用唯一标识
     */
    private String appId;

    /**
     * Namespace ID
     */
    private String namespaceId;

    /**
     * Stage ID
     */
    private String stageId;

    /**
     * Addon 类型
     */
    private ComponentTypeEnum addonType;

    /**
     * Addon 唯一标识
     */
    private String addonId;

    /**
     * Addon 名称
     */
    private String name;

    /**
     * 版本号
     */
    private String addonVersion;

    /**
     * Addon Spec 配置
     */
    private JSONObject spec;
}
