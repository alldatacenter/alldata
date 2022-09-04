package com.alibaba.tesla.appmanager.domain.req.apppackage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 应用包导入请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppPackageImportReq implements Serializable {

    /**
     * 应用 ID
     */
    private String appId;

    /**
     * 包版本
     */
    private String packageVersion;

    /**
     * 包创建者
     */
    private String packageCreator;

    /**
     * 是否强制覆盖已有数据
     */
    private Boolean force;

    /**
     * 重置版本
     */
    private Boolean resetVersion;
}
