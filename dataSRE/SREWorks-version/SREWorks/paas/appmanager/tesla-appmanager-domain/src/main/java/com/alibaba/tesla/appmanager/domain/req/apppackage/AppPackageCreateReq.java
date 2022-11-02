package com.alibaba.tesla.appmanager.domain.req.apppackage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppPackageCreateReq {

    /**
     * 应用 ID
     */
    private String appId;

    /**
     * 包版本
     */
    private String version;

    /**
     * 当前 AppPackage 引用的 components 列表
     */
    private List<Long> componentPackageIdList;
}
