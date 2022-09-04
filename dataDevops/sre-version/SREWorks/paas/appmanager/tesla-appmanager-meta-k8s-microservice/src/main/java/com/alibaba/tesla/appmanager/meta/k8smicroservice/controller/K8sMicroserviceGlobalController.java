package com.alibaba.tesla.appmanager.meta.k8smicroservice.controller;

import com.alibaba.tesla.appmanager.api.provider.K8sMicroServiceMetaProvider;
import com.alibaba.tesla.appmanager.auth.controller.AppManagerBaseController;
import com.alibaba.tesla.appmanager.domain.req.K8sMicroServiceMetaQueryReq;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * K8S 微服务元信息 Controller
 *
 * @author qianmo.zm@alibaba-inc.com
 */
@RequestMapping("/components/k8s-microservices")
@RestController
@Slf4j
public class K8sMicroserviceGlobalController extends AppManagerBaseController {

    @Autowired
    private K8sMicroServiceMetaProvider metaProvider;

    /**
     * @api {get} /apps/:appId/k8s-microservices 获取微服务列表
     * @apiName GetApplicationK8sMicroserviceList
     * @apiGroup 应用关联微服务 API
     * @apiParam (Path Parameters) {String} appId 应用 ID
     * @apiParam (GET Parameters) {String[]} componentTypeList 微服务类型列表 (可选 MICROSERVICE, K8S_MICROSERVICE, K8S_JOB)
     * @apiParam (GET Parameters) {Number} page 当前页
     * @apiParam (GET Parameters) {Number} pageSize 每页大小
     */
    @GetMapping
    public TeslaBaseResult list(@ModelAttribute K8sMicroServiceMetaQueryReq request) {
        request.setWithBlobs(true);
        return buildSucceedResult(metaProvider.list(request));
    }
}
