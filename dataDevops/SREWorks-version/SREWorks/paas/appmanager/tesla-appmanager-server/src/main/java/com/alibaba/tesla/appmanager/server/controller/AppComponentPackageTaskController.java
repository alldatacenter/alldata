package com.alibaba.tesla.appmanager.server.controller;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.api.provider.ComponentPackageProvider;
import com.alibaba.tesla.appmanager.auth.controller.AppManagerBaseController;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.enums.PackageTaskEnum;
import com.alibaba.tesla.appmanager.domain.container.BizAppContainer;
import com.alibaba.tesla.appmanager.domain.dto.ComponentPackageTaskDTO;
import com.alibaba.tesla.appmanager.domain.req.componentpackage.ComponentPackageTaskCreateReq;
import com.alibaba.tesla.appmanager.domain.req.componentpackage.ComponentPackageTaskListQueryReq;
import com.alibaba.tesla.appmanager.domain.res.componentpackage.ComponentPackageCreateRes;
import com.alibaba.tesla.appmanager.server.event.componentpackage.ComponentPackageTaskStartEvent;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * Component Package 任务管理 Controller
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@RequestMapping("/apps/{appId}/component-package-tasks")
@RestController
public class AppComponentPackageTaskController extends AppManagerBaseController {

    @Autowired
    private ComponentPackageProvider componentPackageProvider;

    @Autowired
    private ApplicationEventPublisher publisher;

    // 列出组件打包任务
    @GetMapping
    @ResponseBody
    public TeslaBaseResult list(
            @PathVariable String appId,
            @ModelAttribute ComponentPackageTaskListQueryReq request,
            OAuth2Authentication auth) {
        return buildSucceedResult(componentPackageProvider.listTask(request, getOperator(auth)));
    }

    // 创建组件包打包任务
    @PostMapping
    @ResponseBody
    public TeslaBaseResult create(
            @PathVariable String appId,
            @RequestBody ComponentPackageTaskCreateReq request,
            @RequestHeader(value = "X-Biz-App", required = false) String headerBizApp,
            OAuth2Authentication auth) {
        BizAppContainer container = BizAppContainer.valueOf(headerBizApp);
        String namespaceId = container.getNamespaceId();
        String stageId = container.getStageId();
        if (StringUtils.isEmpty(request.getVersion())) {
            request.setVersion(DefaultConstant.AUTO_VERSION);
        }
        request.setAppId(appId);
        request.setNamespaceId(namespaceId);
        request.setStageId(stageId);
        ComponentPackageCreateRes response = componentPackageProvider.createTask(request, getOperator(auth));
        return buildSucceedResult(response);
    }

    // 获取组件包详情
    @GetMapping("/{taskId}")
    @ResponseBody
    public TeslaBaseResult get(
            @PathVariable String appId, @PathVariable("taskId") Long taskId,
            HttpServletRequest r, OAuth2Authentication auth) {
        return buildSucceedResult(componentPackageProvider.getTask(taskId, getOperator(auth)));
    }

    // 查询日志
    @GetMapping("/{taskId}/logs")
    @ResponseBody
    public TeslaBaseResult logs(
            @PathVariable String appId, @PathVariable("taskId") Long taskId,
            HttpServletRequest r, OAuth2Authentication auth) {
        ComponentPackageTaskDTO response = componentPackageProvider.getTask(taskId, getOperator(auth));
        String taskLog = response.getTaskLog();
        if (taskLog == null) {
            taskLog = "";
        }
        JSONObject params = new JSONObject();
        params.put("params", new JSONObject(ImmutableMap.<String, Object>builder()
                .put("log", taskLog)
                .build()));
        return buildSucceedResult(params);
    }

    // 重试指定任务
    @PostMapping(value = "/{taskId}/retry")
    @ResponseBody
    public TeslaBaseResult retryTask(
            @PathVariable String appId,
            @PathVariable("taskId") Long taskId,
            @RequestHeader(value = "X-Biz-App", required = false) String headerBizApp,
            OAuth2Authentication auth) {
        BizAppContainer container = BizAppContainer.valueOf(headerBizApp);
        String namespaceId = container.getNamespaceId();
        String stageId = container.getStageId();
        publisher.publishEvent(new ComponentPackageTaskStartEvent(
                this, 0L, taskId, appId, namespaceId, stageId, getOperator(auth), null, PackageTaskEnum.RETRY));
        return buildSucceedResult(Boolean.TRUE);
    }
}
