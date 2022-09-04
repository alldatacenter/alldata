package com.alibaba.tesla.appmanager.domain.dto;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.common.enums.PackageTypeEnum;
import lombok.Data;

import java.util.Date;

/**
 * HELM组件DTO
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/12/24 17:37
 */
@Data
public class HelmMetaDTO {
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
     * 应用 ID
     */
    private String appId;

    /**
     * Helm 包标识 ID
     */
    private String helmPackageId;

    /**
     * Helm 名称
     */
    private String name;

    /**
     * 描述信息
     */
    private String description;

    /**
     * 组件类型
     */
    private ComponentTypeEnum componentType;

    /**
     * 包类型
     */
    private PackageTypeEnum packageType;

    /**
     * Helm 扩展信息
     */
    private JSONObject helmExt;

    /**
     * 构建 Options 信息
     */
    private String options;
}
