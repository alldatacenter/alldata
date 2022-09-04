package com.alibaba.tesla.appmanager.server.controller;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.api.provider.DeployAppProvider;
import com.alibaba.tesla.appmanager.auth.controller.AppManagerBaseController;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.dto.DeployAppAttrDTO;
import com.alibaba.tesla.appmanager.domain.dto.DeployAppDTO;
import com.alibaba.tesla.appmanager.domain.dto.DeployComponentAttrDTO;
import com.alibaba.tesla.appmanager.domain.req.deploy.*;
import com.alibaba.tesla.appmanager.domain.res.deploy.DeployAppPackageLaunchRes;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;

/**
 * 部署单管理
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@RequestMapping("/deployments")
@RestController
public class DeploymentController extends AppManagerBaseController {

    @Autowired
    private DeployAppProvider deployAppProvider;

    // 发起部署
    @PostMapping(value = "/launch")
    @ResponseBody
    public TeslaBaseResult launch(
            @Valid @ModelAttribute DeployAppLaunchReq request,
            @RequestBody String body, OAuth2Authentication auth
    ) {
        request.setConfiguration(body);
        try {
            DeployAppPackageLaunchRes response = deployAppProvider.launch(request, getOperator(auth));
            return buildSucceedResult(response);
        } catch (Exception e) {
            log.error("cannot launch deployments|exception={}|yaml={}", ExceptionUtils.getStackTrace(e), body);
            return buildExceptionResult(e);
        }
    }

    // 发起部署
    @PostMapping(value = "/fast-launch")
    @ResponseBody
    public TeslaBaseResult fastLaunch(@RequestBody FastDeployAppLaunchReq request, OAuth2Authentication auth) {
        try {
            DeployAppPackageLaunchRes response = deployAppProvider.fastLaunch(request, getOperator(auth));
            return buildSucceedResult(response);
        } catch (Exception e) {
            log.error("cannot launch deployments|exception={}|request={}",
                    ExceptionUtils.getStackTrace(e), JSONObject.toJSONString(request));
            return buildExceptionResult(e);
        }
    }

    // 根据过滤条件查询部署单详情
    @GetMapping
    @ResponseBody
    public TeslaBaseResult list(
            @ModelAttribute DeployAppListReq request, OAuth2Authentication auth
    ) throws Exception {
        Pagination<DeployAppDTO> response = deployAppProvider.list(request, getOperator(auth));
        return buildSucceedResult(response);
    }

    // 重新部署当前部署单
    @PostMapping("{deployAppId}/replay")
    @ResponseBody
    public TeslaBaseResult replay(
            @PathVariable("deployAppId") Long deployAppId, OAuth2Authentication auth
    ) throws Exception {
        DeployAppReplayReq request = DeployAppReplayReq.builder()
                .deployAppId(deployAppId)
                .build();
        try {
            DeployAppPackageLaunchRes response = deployAppProvider.replay(request, getOperator(auth));
            return buildSucceedResult(response);
        } catch (Exception e) {
            log.error("cannot launch deployments|exception={}|originDeployId={}",
                    ExceptionUtils.getStackTrace(e), deployAppId);
            return buildExceptionResult(e);
        }
    }

    // 查询指定部署单详情
    @GetMapping("{deployAppId}")
    @ResponseBody
    public TeslaBaseResult get(
            @PathVariable("deployAppId") Long deployAppId, OAuth2Authentication auth
    ) throws Exception {
        if (deployAppId == 0) {
            return buildSucceedResult(new DeployAppDTO());
        }
        
        DeployAppGetReq request = DeployAppGetReq.builder()
                .deployAppId(deployAppId)
                .build();
        DeployAppDTO response = deployAppProvider.get(request, getOperator(auth));
        return buildSucceedResult(response);
    }

    // 查询指定部署单详情
    @GetMapping("{deployAppId}/attributes")
    @ResponseBody
    public TeslaBaseResult getAttributes(
            @PathVariable("deployAppId") Long deployAppId, OAuth2Authentication auth
    ) throws Exception {
        DeployAppGetAttrReq request = DeployAppGetAttrReq.builder()
                .deployAppId(deployAppId)
                .build();
        DeployAppAttrDTO response = deployAppProvider.getAttr(request, getOperator(auth));
        return buildSucceedResult(response);
    }

    // 查询指定部署单的指定步骤详情
    @GetMapping("{deployAppId}/components/{deployComponentId}/attributes")
    @ResponseBody
    public TeslaBaseResult getComponentAttributes(
            @PathVariable("deployAppId") Long deployAppId,
            @PathVariable("deployComponentId") Long deployComponentId,
            OAuth2Authentication auth
    ) {
        DeployAppGetComponentAttrReq request = DeployAppGetComponentAttrReq.builder()
                .deployComponentId(deployComponentId)
                .build();
        DeployComponentAttrDTO response = deployAppProvider.getComponentAttr(request, getOperator(auth));
        return buildSucceedResult(response);
    }

    // 重试指定部署单
    @PostMapping("{deployAppId}/retry")
    @ResponseBody
    public TeslaBaseResult retry(
            @PathVariable("deployAppId") Long deployAppId, OAuth2Authentication auth) {
        DeployAppRetryReq request = DeployAppRetryReq.builder()
                .deployAppId(deployAppId)
                .build();
        deployAppProvider.retry(request, getOperator(auth));
        return buildSucceedResult(new HashMap<String, String>());
    }

    // 终止指定部署单
    @PostMapping("{deployAppId}/terminate")
    @ResponseBody
    public TeslaBaseResult terminate(
            @PathVariable("deployAppId") Long deployAppId, OAuth2Authentication auth) {
        DeployAppTerminateReq request = DeployAppTerminateReq.builder()
                .deployAppId(deployAppId)
                .build();
        deployAppProvider.terminate(request, getOperator(auth));
        return buildSucceedResult(new HashMap<String, String>());
    }
}
