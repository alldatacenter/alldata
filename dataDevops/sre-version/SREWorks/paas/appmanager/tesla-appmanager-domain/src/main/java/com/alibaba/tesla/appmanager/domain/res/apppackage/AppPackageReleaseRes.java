package com.alibaba.tesla.appmanager.domain.res.apppackage;

import lombok.Builder;
import lombok.Data;

/**
 * @ClassName:AppPackageReleaseRes
 * @author yangjie.dyj@alibaba-inc.com
 * @DATE: 2020-11-11
 * @Description:
 **/
@Builder
@Data
public class AppPackageReleaseRes {
    /**
     * AppPackage发布id
     */
    private Long appPackageRelaseId;
}
