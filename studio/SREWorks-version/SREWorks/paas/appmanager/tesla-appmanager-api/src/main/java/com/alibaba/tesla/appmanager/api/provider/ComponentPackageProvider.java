package com.alibaba.tesla.appmanager.api.provider;

import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.dto.ComponentPackageDTO;
import com.alibaba.tesla.appmanager.domain.dto.ComponentPackageTaskDTO;
import com.alibaba.tesla.appmanager.domain.dto.ComponentPackageVersionItemDTO;
import com.alibaba.tesla.appmanager.domain.req.componentpackage.*;
import com.alibaba.tesla.appmanager.domain.res.componentpackage.ComponentPackageCreateRes;

import java.util.List;

/**
 * Component Package 服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface ComponentPackageProvider {

    /**
     * 查询 ComponentPackage 列表
     */
    Pagination<ComponentPackageDTO> list(ComponentPackageQueryReq request, String operator);

    /**
     * 根据 appPackageId 查询 ComponentPackage 列表
     */
    Pagination<ComponentPackageDTO> listByAppPackageId(ComponentPackageListQueryReq request, String operator);

    /**
     * 创建 Component Package
     */
    ComponentPackageCreateRes createTask(ComponentPackageTaskCreateReq request, String operator);

    /**
     * 查询指定任务状态
     */
    ComponentPackageTaskDTO getTask(ComponentPackageTaskQueryReq request, String operator);

    /**
     * 根据 TaskID 查询任务
     */
    ComponentPackageTaskDTO getTask(Long componentPackageTaskId, String operator);

    /**
     * 查询指定应用打包任务ID的组件打包任务
     */
    Pagination<ComponentPackageTaskDTO> listTask(ComponentPackageTaskListQueryReq request, String operator);

    /**
     * 重试指定任务
     */
    void retryTask(ComponentPackageTaskRetryReq request, String operator);

    /**
     * 获取指定应用包的最新版本列表
     */
    List<ComponentPackageVersionItemDTO> latestVersions(ComponentPackageLatestVersionListReq request, String operator);
}
