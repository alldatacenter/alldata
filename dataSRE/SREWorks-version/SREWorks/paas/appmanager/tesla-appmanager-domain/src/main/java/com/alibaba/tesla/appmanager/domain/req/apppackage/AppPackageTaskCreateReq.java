package com.alibaba.tesla.appmanager.domain.req.apppackage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 创建 Component Package 请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppPackageTaskCreateReq {

    /**
     * 应用 ID
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
     * 包版本
     */
    private String version;

    /**
     * 当前 AppPackage 的标签列表
     */
    private List<String> tags;

    /**
     * 当前 AppPackage 引用的 components 列表
     */
    private List<ComponentBinder> components;

    /**
     * 是否保存当前系统中的 Deploy Config 到组件包中
     */
    private boolean storeConfiguration = true;

    /**
     * 是否包含开发态内容
     */
    private boolean develop;
}
