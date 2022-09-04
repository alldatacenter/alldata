package com.alibaba.tesla.appmanager.domain.req;

import com.alibaba.tesla.appmanager.common.BaseRequest;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import lombok.Data;

import java.util.List;

/**
 * 插件元数据查询请求
 *
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Data
public class AddonMetaQueryReq extends BaseRequest {

    /**
     * 插件类型
     */
    private List<ComponentTypeEnum> addonTypeList;

    /**
     * Addon ID
     */
    private String addonId;

    /**
     * Addon Version
     */
    private String addonVersion;
}
