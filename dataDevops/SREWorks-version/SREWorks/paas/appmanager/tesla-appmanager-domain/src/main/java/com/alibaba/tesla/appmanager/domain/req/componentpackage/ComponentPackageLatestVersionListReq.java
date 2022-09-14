package com.alibaba.tesla.appmanager.domain.req.componentpackage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 组件包获取最新版本号
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentPackageLatestVersionListReq {

    /**
     * 应用 ID
     */
    private String appId;

    /**
     * 组件类型
     */
    private String componentType;

    /**
     * 组件名称
     */
    private String componentName;
}
