package com.alibaba.tesla.appmanager.meta.helm.controller;

import com.alibaba.tesla.appmanager.api.provider.HelmMetaProvider;
import com.alibaba.tesla.appmanager.auth.controller.AppManagerBaseController;
import com.alibaba.tesla.appmanager.domain.container.BizAppContainer;
import com.alibaba.tesla.appmanager.domain.req.helm.HelmMetaQueryReq;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/components/helm")
@Slf4j
public class HelmGlobalController extends AppManagerBaseController {

    @Autowired
    private HelmMetaProvider metaProvider;

    /**
     * @api {get} /components/helm 获取应用HELM组件列表
     * @apiName GetApplicationHelmMetaList
     * @apiGroup 应用关联HELM组件 API
     * @apiParam (Path Parameters) {String} appId 应用ID
     * @apiParam (GET Parameters) {String} id HELM组件主键ID
     * @apiParam (GET Parameters) {String} name HELM组件名称
     * @apiParam (GET Parameters) {Number} page 当前页
     * @apiParam (GET Parameters) {Number} pageSize 每页大小
     */
    @GetMapping
    public TeslaBaseResult list(
            @ModelAttribute HelmMetaQueryReq request,
            @RequestHeader(value = "X-Biz-App", required = false) String headerBizApp) {
        BizAppContainer container = BizAppContainer.valueOf(headerBizApp);
        String namespaceId = container.getNamespaceId();
        String stageId = container.getStageId();
        request.setNamespaceId(namespaceId);
        request.setStageId(stageId);
        request.setWithBlobs(true);
        return buildSucceedResult(metaProvider.list(request));
    }
}
