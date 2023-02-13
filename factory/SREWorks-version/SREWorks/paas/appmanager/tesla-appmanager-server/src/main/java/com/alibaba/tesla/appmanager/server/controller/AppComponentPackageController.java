package com.alibaba.tesla.appmanager.server.controller;

import com.alibaba.tesla.appmanager.api.provider.ComponentPackageProvider;
import com.alibaba.tesla.appmanager.auth.controller.AppManagerBaseController;
import com.alibaba.tesla.appmanager.domain.req.componentpackage.ComponentPackageLatestVersionListReq;
import com.alibaba.tesla.appmanager.domain.req.componentpackage.ComponentPackageQueryReq;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Component Package 管理
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@RequestMapping("/apps/{appId}/component-packages")
@RestController
public class AppComponentPackageController extends AppManagerBaseController {

    @Autowired
    private ComponentPackageProvider componentPackageProvider;

    /**
     * @api {get} /apps/:appId/component-packages 获取应用下的组件包列表
     * @apiName GetApplicationComponentPackageList
     * @apiGroup 应用组件包 API
     * @apiParam (Path Parameters) {String} appId 应用 ID
     * @apiParam (GET Parameters) {Number} id 过滤条件：组件包 ID
     * @apiParam (GET Parameters) {String} componentType 过滤条件：组件类型
     * @apiParam (GET Parameters) {String} componentName 过滤条件：组件名称
     * @apiParam (GET Parameters) {String} packageVersion 过滤条件：包版本号
     */
    @GetMapping
    public TeslaBaseResult list(
            @PathVariable String appId, @ModelAttribute ComponentPackageQueryReq request,
            OAuth2Authentication auth) {
        request.setAppId(appId);
        return buildSucceedResult(componentPackageProvider.list(request, getOperator(auth)));
    }

    /**
     * @api {get} /apps/:appId/component-packages/:componentType/:componentName/latest-version 获取应用下指定组件最新版本的组件包
     * @apiName GetApplicationComponentPackageLatestVersion
     * @apiGroup 应用组件包 API
     * @apiParam (Path Parameters) {String} appId 应用 ID
     * @apiParam (Path Parameters) {String} componentType 组件类型
     * @apiParam (Path Parameters) {String} componentName 组件名称
     */
    @GetMapping("/{componentType}/{componentName}/latest-version")
    public TeslaBaseResult latestVersion(
            @PathVariable String appId,
            @PathVariable String componentType,
            @PathVariable String componentName,
            OAuth2Authentication auth) {
        ComponentPackageLatestVersionListReq request = ComponentPackageLatestVersionListReq.builder()
                .appId(appId)
                .componentType(componentType)
                .componentName(componentName)
                .build();
        return buildSucceedResult(componentPackageProvider.latestVersions(request, getOperator(auth)));
    }
}
