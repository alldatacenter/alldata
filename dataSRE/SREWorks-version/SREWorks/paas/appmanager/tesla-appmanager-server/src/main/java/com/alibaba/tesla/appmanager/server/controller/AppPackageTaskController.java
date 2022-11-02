package com.alibaba.tesla.appmanager.server.controller;

import com.alibaba.tesla.appmanager.api.provider.AppComponentProvider;
import com.alibaba.tesla.appmanager.api.provider.AppPackageTaskProvider;
import com.alibaba.tesla.appmanager.api.provider.ComponentPackageProvider;
import com.alibaba.tesla.appmanager.auth.controller.AppManagerBaseController;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.container.BizAppContainer;
import com.alibaba.tesla.appmanager.domain.dto.AppComponentDTO;
import com.alibaba.tesla.appmanager.domain.dto.AppPackageTaskDTO;
import com.alibaba.tesla.appmanager.domain.dto.ComponentPackageVersionItemDTO;
import com.alibaba.tesla.appmanager.domain.req.appcomponent.AppComponentQueryReq;
import com.alibaba.tesla.appmanager.domain.req.apppackage.AppPackageTaskCreateReq;
import com.alibaba.tesla.appmanager.domain.req.apppackage.AppPackageTaskNextLatestVersionReq;
import com.alibaba.tesla.appmanager.domain.req.apppackage.AppPackageTaskQueryReq;
import com.alibaba.tesla.appmanager.domain.req.apppackage.ComponentBinder;
import com.alibaba.tesla.appmanager.domain.req.componentpackage.ComponentPackageLatestVersionListReq;
import com.alibaba.tesla.appmanager.domain.res.apppackage.AppPackageTaskCreateRes;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

import static com.alibaba.tesla.appmanager.common.constants.DefaultConstant.INTERNAL_ADDON_APP_META;
import static com.alibaba.tesla.appmanager.common.constants.DefaultConstant.INTERNAL_ADDON_DEVELOPMENT_META;

/**
 * App Package 任务管理 Controller
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@RequestMapping
@RestController
public class AppPackageTaskController extends AppManagerBaseController {

    @Autowired
    private AppPackageTaskProvider appPackageTaskProvider;

    @Autowired
    private AppComponentProvider appComponentProvider;

    @Autowired
    private ComponentPackageProvider componentPackageProvider;

    /**
     * @api {post} /apps/:appId/app-package-tasks 创建应用包打包任务
     * @apiName PostApplicationPackageTask
     * @apiGroup 应用包任务 API
     * @apiParam (Path Parameters) {String} appId 应用 ID
     * @apiParam (JSON Body) {String} version 版本号
     * @apiParam (JSON Body) {String[]} tags 标签列表
     * @apiParam (JSON Body) {Object[]} components 组件列表
     */
    @PostMapping("/apps/{appId}/app-package-tasks")
    @ResponseBody
    public TeslaBaseResult create(
            @PathVariable String appId,
            @RequestBody AppPackageTaskCreateReq request,
            @RequestHeader(value = "X-Biz-App", required = false) String headerBizApp,
            OAuth2Authentication auth) {
        if (CollectionUtils.isEmpty(request.getTags())) {
            return buildClientErrorResult("tags is required");
        }

        BizAppContainer container = BizAppContainer.valueOf(headerBizApp);
        String namespaceId = container.getNamespaceId();
        String stageId = container.getStageId();
        request.setAppId(appId);
        request.setNamespaceId(namespaceId);
        request.setStageId(stageId);
        try {
            AppPackageTaskCreateRes response = appPackageTaskProvider.create(request, getOperator(auth));
            return buildSucceedResult(response);
        } catch (AppException e) {
            return buildResult(e.getErrorCode().getCode(), e.getErrorMessage(), e.getErrorData());
        }
    }

    /**
     * @api {get} /apps/:appId/app-package-tasks 获取应用包任务列表
     * @apiName GetApplicationPackageTaskList
     * @apiGroup 应用包任务 API
     * @apiParam (Path Parameters) {String} appId 应用 ID
     */
    @GetMapping("/apps/{appId}/app-package-tasks")
    @ResponseBody
    public TeslaBaseResult list(@PathVariable String appId, OAuth2Authentication auth) {
        AppPackageTaskQueryReq request = AppPackageTaskQueryReq.builder()
                .appId(appId)
                .build();
        return buildSucceedResult(appPackageTaskProvider.list(request, getOperator(auth), true));
    }

    /**
     * @api {get} /apps/:appId/app-package-tasks 获取应用包任务列表
     * @apiName GetApplicationPackageTaskList
     * @apiGroup 应用包任务 API
     * @apiParam (Path Parameters) {String} appId 应用 ID
     */
    @GetMapping("/app-package-tasks")
    @ResponseBody
    public TeslaBaseResult listCompatible(@ModelAttribute AppPackageTaskQueryReq request, OAuth2Authentication auth) {
        return buildSucceedResult(appPackageTaskProvider.list(
                AppPackageTaskQueryReq.builder().appId(request.getAppId()).build(),
                getOperator(auth), true));
    }

    /**
     * @api {get} /apps/:appId/app-package-tasks/:taskId 获取指定应用包任务详情
     * @apiName GetApplicationPackageTask
     * @apiGroup 应用包任务 API
     * @apiParam (Path Parameters) {String} appId 应用 ID
     * @apiParam (Path Parameters) {Number} taskId 任务 ID
     */
    @GetMapping("/apps/{appId}/app-package-tasks/{taskId}")
    @ResponseBody
    public TeslaBaseResult get(
            @PathVariable String appId, @PathVariable Long taskId,
            OAuth2Authentication auth) {
        AppPackageTaskQueryReq request = AppPackageTaskQueryReq.builder()
                .appId(appId)
                .appPackageTaskId(taskId)
                .withBlobs(true)
                .build();
        Pagination<AppPackageTaskDTO> tasks = appPackageTaskProvider.list(request, getOperator(auth), true);
        if (!tasks.isEmpty()) {
            return buildSucceedResult(tasks.getItems().get(0));
        }
        return buildClientErrorResult(AppErrorCode.INVALID_USER_ARGS.getDescription());
    }

    @PostMapping("/apps/{appId}/app-package-tasks/quick-create")
    @ResponseBody
    public TeslaBaseResult quickCreate(
            @PathVariable String appId,
            @RequestBody AppPackageTaskCreateReq request,
            @RequestHeader(value = "X-Biz-App", required = false) String headerBizApp,
            OAuth2Authentication auth) {
        if (CollectionUtils.isEmpty(request.getTags())) {
            return buildClientErrorResult("tags is required");
        }
        BizAppContainer container = BizAppContainer.valueOf(headerBizApp);
        String namespaceId = container.getNamespaceId();
        String stageId = container.getStageId();
        String operator = getOperator(auth);

        try {
            String appPackageVersion = appPackageTaskProvider.nextLatestVersion(
                    AppPackageTaskNextLatestVersionReq.builder().appId(appId).build(), operator
            );
            request.setVersion(appPackageVersion);
            List<AppComponentDTO> appComponents = appComponentProvider.list(
                    AppComponentQueryReq.builder()
                            .appId(appId)
                            .namespaceId(namespaceId)
                            .stageId(stageId)
                            .build(), operator
            );
            if (CollectionUtils.isEmpty(appComponents)) {
                throw new AppException(AppErrorCode.INVALID_USER_ARGS, "missing package component");
            }

            List<ComponentBinder> components = new ArrayList<>();
            for (AppComponentDTO appComponent : appComponents) {
                ComponentBinder componentBinder = ComponentBinder.builder()
                        .componentType(appComponent.getComponentType())
                        .componentName(appComponent.getComponentName())
                        .componentLabel(appComponent.getComponentLabel())
                        .version(appComponent.getComponentVersion())
                        .isDevelop(request.isDevelop())
                        .build();
                if (appComponent.getComponentType().isKubernetesMicroservice()) {
                    componentBinder.setBranch(DefaultConstant.DEFAULT_REPO_BRANCH);
                    List<ComponentPackageVersionItemDTO> componentVersionList = componentPackageProvider
                            .latestVersions(
                                    ComponentPackageLatestVersionListReq.builder()
                                            .appId(appId)
                                            .componentType(appComponent.getComponentType().toString())
                                            .componentName(appComponent.getComponentName())
                                            .build(),
                                    operator);
                    if (CollectionUtils.isEmpty(componentVersionList)) {
                        return buildClientErrorResult(appComponent.getComponentName() + " 最新版本号缺失");
                    }
                    componentBinder.setVersion(componentVersionList.get(0).getName());
                } else if (appComponent.getComponentType().isHelm()) {
                    componentBinder.setBranch(DefaultConstant.DEFAULT_REPO_BRANCH);
                    List<ComponentPackageVersionItemDTO> componentVersionList = componentPackageProvider
                            .latestVersions(
                                    ComponentPackageLatestVersionListReq.builder()
                                            .appId(appId)
                                            .componentType(appComponent.getComponentType().toString())
                                            .componentName(appComponent.getComponentName())
                                            .build(),
                                    operator);
                    if (CollectionUtils.isEmpty(componentVersionList)) {
                        return buildClientErrorResult(appComponent.getComponentName() + " 最新版本号缺失");
                    }
                    componentBinder.setVersion(componentVersionList.get(0).getName());
                }
                components.add(componentBinder);
            }

            if (request.isDevelop()) {
                ComponentBinder developmentMeta = ComponentBinder.builder()
                        .componentType(ComponentTypeEnum.INTERNAL_ADDON)
                        .componentName(INTERNAL_ADDON_DEVELOPMENT_META)
                        .componentLabel("Development Meta")
                        .version(DefaultConstant.INIT_VERSION)
                        .build();
                components.add(developmentMeta);

                ComponentBinder appMeta = ComponentBinder.builder()
                        .componentType(ComponentTypeEnum.INTERNAL_ADDON)
                        .componentName(INTERNAL_ADDON_APP_META)
                        .componentLabel("App Meta")
                        .version(DefaultConstant.INIT_VERSION)
                        .build();
                components.add(appMeta);
            }

            request.setComponents(components);
            request.setAppId(appId);
            request.setNamespaceId(namespaceId);
            request.setStageId(stageId);
            AppPackageTaskCreateRes response = appPackageTaskProvider.create(request, getOperator(auth));
            return buildSucceedResult(response);
        } catch (AppException e) {
            return buildResult(e.getErrorCode().getCode(), e.getErrorMessage(), e.getErrorData());
        }
    }
}
