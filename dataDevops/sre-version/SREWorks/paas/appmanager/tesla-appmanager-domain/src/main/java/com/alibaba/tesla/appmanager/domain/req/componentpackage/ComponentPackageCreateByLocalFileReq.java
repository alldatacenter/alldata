package com.alibaba.tesla.appmanager.domain.req.componentpackage;

import com.alibaba.tesla.appmanager.domain.schema.AppPackageSchema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentPackageCreateByLocalFileReq {

    /**
     * 应用 ID
     */
    private String appId;

    /**
     * 组件包单个对象
     */
    private AppPackageSchema.ComponentPackageItem componentPackageItem;

    /**
     * 本地文件路径
     */
    private String localFilePath;

    /**
     * 是否强制导入
     */
    private boolean force;

    /**
     * 是否重置版本
     */
    private boolean resetVersion;
}
