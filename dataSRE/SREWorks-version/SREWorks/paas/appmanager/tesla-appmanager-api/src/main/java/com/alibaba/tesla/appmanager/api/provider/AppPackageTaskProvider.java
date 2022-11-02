package com.alibaba.tesla.appmanager.api.provider;

import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.dto.AppPackageTaskDTO;
import com.alibaba.tesla.appmanager.domain.req.apppackage.AppPackageTaskCreateReq;
import com.alibaba.tesla.appmanager.domain.req.apppackage.AppPackageTaskNextLatestVersionReq;
import com.alibaba.tesla.appmanager.domain.req.apppackage.AppPackageTaskQueryReq;
import com.alibaba.tesla.appmanager.domain.res.apppackage.AppPackageTaskCreateRes;

/**
 * 应用包任务 Provider
 *
 * @author qianmo.zm@alibaba-inc.com
 */
public interface AppPackageTaskProvider {

    /**
     * 创建 App Package 任务
     */
    AppPackageTaskCreateRes create(AppPackageTaskCreateReq request, String operator);

    /**
     * 查询 App Package 任务
     */
    Pagination<AppPackageTaskDTO> list(AppPackageTaskQueryReq request, String operator, boolean withTags);

    /**
     * 获取指定 App Package 任务
     */
    AppPackageTaskDTO get(AppPackageTaskQueryReq request, String operator);

    /**
     * 获取指定 appId 对应的当前最新版本
     */
    String nextLatestVersion(AppPackageTaskNextLatestVersionReq request, String operator);
}
