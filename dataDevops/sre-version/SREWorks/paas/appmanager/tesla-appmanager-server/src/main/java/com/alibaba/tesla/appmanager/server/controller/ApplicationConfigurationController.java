package com.alibaba.tesla.appmanager.server.controller;

import com.alibaba.tesla.appmanager.api.provider.DeployConfigProvider;
import com.alibaba.tesla.appmanager.auth.controller.AppManagerBaseController;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.domain.container.BizAppContainer;
import com.alibaba.tesla.appmanager.domain.req.deployconfig.DeployConfigApplyTemplateReq;
import com.alibaba.tesla.appmanager.domain.req.deployconfig.DeployConfigDeleteReq;
import com.alibaba.tesla.appmanager.domain.req.deployconfig.DeployConfigGenerateReq;
import com.alibaba.tesla.appmanager.domain.res.apppackage.ApplicationConfigurationGenerateRes;
import com.alibaba.tesla.appmanager.domain.res.deployconfig.DeployConfigGenerateRes;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Application Configuration Controller
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@RequestMapping("/application-configurations")
@RestController
@Slf4j
public class ApplicationConfigurationController extends AppManagerBaseController {

    @Autowired
    private DeployConfigProvider deployConfigProvider;

    /**
     * @api {put} /application-configurations 更新全局部署信息
     * @apiName PutApplicationConfigurations
     * @apiGroup Application Configuration API
     */
    @PutMapping
    public TeslaBaseResult update(
            @RequestBody DeployConfigApplyTemplateReq request,
            @RequestHeader(value = "X-Biz-App", required = false) String headerBizApp,
            OAuth2Authentication auth) {
        if (StringUtils.isEmpty(request.getAppId())) {
            request.setAppId("");
        } else if (StringUtils.isEmpty(request.getEnvId())) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "empty envId is not allowed");
        }

        BizAppContainer container = BizAppContainer.valueOf(headerBizApp);
        request.setIsolateNamespaceId(container.getNamespaceId());
        request.setIsolateStageId(container.getStageId());
        return buildSucceedResult(deployConfigProvider.applyTemplate(request));
    }

    /**
     * @api {get} /application-configurations 获取全局部署信息
     * @apiName GetApplicationConfigurations
     * @apiGroup Application Configuration API
     */
    @GetMapping
    public TeslaBaseResult get(
            @ModelAttribute DeployConfigGenerateReq request,
            @RequestHeader(value = "X-Biz-App", required = false) String headerBizApp,
            OAuth2Authentication auth) {
        if (StringUtils.isEmpty(request.getApiVersion())) {
            request.setApiVersion(DefaultConstant.API_VERSION_V1_ALPHA2);
        }
        if (StringUtils.isEmpty(request.getAppId())) {
            request.setAppId("");
        }
        request.setAppPackageId(0L);

        BizAppContainer container = BizAppContainer.valueOf(headerBizApp);
        request.setIsolateNamespaceId(container.getNamespaceId());
        request.setIsolateStageId(container.getStageId());
        DeployConfigGenerateRes result = deployConfigProvider.generate(request);
        return buildSucceedResult(ApplicationConfigurationGenerateRes.builder()
                .yaml(SchemaUtil.toYamlMapStr(result.getSchema()))
                .build());
    }

    /**
     * @api {delete} /application-configurations 获取全局部署信息
     * @apiName GetApplicationConfigurations
     * @apiGroup Application Configuration API
     */
    @DeleteMapping
    public TeslaBaseResult delete(
            @ModelAttribute DeployConfigDeleteReq request,
            @RequestHeader(value = "X-Biz-App", required = false) String headerBizApp,
            OAuth2Authentication auth) {
        if (StringUtils.isEmpty(request.getApiVersion())) {
            request.setApiVersion(DefaultConstant.API_VERSION_V1_ALPHA2);
        }
        if (StringUtils.isEmpty(request.getAppId())) {
            request.setAppId("");
        }
        BizAppContainer container = BizAppContainer.valueOf(headerBizApp);
        String namespaceId = container.getNamespaceId();
        String stageId = container.getStageId();
        request.setIsolateNamespaceId(namespaceId);
        request.setIsolateStageId(stageId);
        deployConfigProvider.delete(request);
        return buildSucceedResult(DefaultConstant.EMPTY_OBJ);
    }
}
