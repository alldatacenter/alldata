package com.alibaba.tesla.appmanager.api.provider;

import com.alibaba.tesla.appmanager.domain.req.productrelease.CreateAppPackageTaskInProductReleaseTaskReq;
import com.alibaba.tesla.appmanager.domain.res.productrelease.CreateAppPackageTaskInProductReleaseTaskRes;

/**
 * 产品与发布版本 Provider
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface ProductReleaseProvider {

    /**
     * 获取指定 productId + releaseId + appId 的 launch yaml 文件内容
     *
     * @param productId 产品 ID
     * @param releaseId 发布版本 ID
     * @param appId     应用 ID
     * @return launch yaml 文件内容
     */
    String getLaunchYaml(String productId, String releaseId, String appId);

    /**
     * 在产品发布版本任务中创建 AppPackage 打包任务
     *
     * @param request 请求内容
     * @return 应用包任务 ID
     */
    CreateAppPackageTaskInProductReleaseTaskRes createAppPackageTaskInProductReleaseTask(
            CreateAppPackageTaskInProductReleaseTaskReq request);
}
