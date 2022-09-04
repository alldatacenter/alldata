package com.alibaba.tesla.appmanager.workflow.util;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.api.provider.DeployAppProvider;
import com.alibaba.tesla.appmanager.api.provider.UnitProvider;
import com.alibaba.tesla.appmanager.common.enums.DynamicScriptKindEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.domain.req.deploy.DeployAppLaunchReq;
import com.alibaba.tesla.appmanager.domain.res.deploy.DeployAppPackageLaunchRes;
import com.alibaba.tesla.appmanager.domain.schema.DeployAppSchema;
import com.alibaba.tesla.appmanager.dynamicscript.core.GroovyHandlerFactory;
import com.alibaba.tesla.appmanager.workflow.dynamicscript.PolicyHandler;
import com.alibaba.tesla.dag.common.BeanUtil;
import org.apache.commons.lang.exception.ExceptionUtils;

/**
 * Workflow Handler 工具类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class WorkflowHandlerUtil {

    /**
     * 获取指定 policyType 对应的 PolicyHandler
     *
     * @param policyType Policy Type
     * @return Policy Handler
     */
    public static PolicyHandler getPolicyHandler(String policyType) {
        GroovyHandlerFactory groovyHandlerFactory = BeanUtil.getBean(GroovyHandlerFactory.class);
        if (groovyHandlerFactory == null) {
            throw new AppException(AppErrorCode.UNKNOWN_ERROR, "cannot find GroovyHandlerFactory bean");
        }
        PolicyHandler policyHandler;
        try {
            policyHandler = groovyHandlerFactory.get(PolicyHandler.class,
                    DynamicScriptKindEnum.POLICY.toString(), policyType);
        } catch (Exception e) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    "cannot find policy handler by type " + policyType, e);
        }
        return policyHandler;
    }

    /**
     * 获取 configuration 中的 Policy 对象
     *
     * @param configuration 部署配置
     * @param policyName    Policy Name
     * @return Policy Properties
     */
    public static DeployAppSchema.Policy getPolicy(DeployAppSchema configuration, String policyName) {
        for (DeployAppSchema.Policy policy : configuration.getSpec().getPolicies()) {
            if (policyName.equals(policy.getName())) {
                return policy;
            }
        }
        throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                String.format("cannot find policy properties by policyName %s", policyName));
    }

    /**
     * 执行部署，并返回部署单 ID
     *
     * @param configuration 部署配置文件
     * @param creator       创建者
     * @return 部署单 ID
     */
    public static Long deploy(DeployAppSchema configuration, String creator) {
        DeployAppProvider deployAppProvider = BeanUtil.getBean(DeployAppProvider.class);
        if (deployAppProvider == null) {
            throw new AppException(AppErrorCode.UNKNOWN_ERROR, "cannot find DeployAppProvider bean");
        }
        DeployAppLaunchReq req = DeployAppLaunchReq.builder()
                .configuration(SchemaUtil.toYamlMapStr(configuration))
                .build();
        DeployAppPackageLaunchRes res = deployAppProvider.launch(req, creator);
        return res.getDeployAppId();
    }

    /**
     * 执行远端部署，并返回部署单 ID
     *
     * @param unitId        单元 ID
     * @param configuration 部署配置文件
     * @return 部署单 ID
     */
    public static Long deployRemoteUnit(String unitId, DeployAppSchema configuration) {
        UnitProvider unitProvider = BeanUtil.getBean(UnitProvider.class);
        if (unitProvider == null) {
            throw new AppException(AppErrorCode.UNKNOWN_ERROR, "cannot find UnitProvider bean");
        }
        DeployAppLaunchReq req = DeployAppLaunchReq.builder()
                .configuration(SchemaUtil.toYamlMapStr(configuration))
                .build();
        try {
            JSONObject res = unitProvider.launchDeployment(unitId, req);
            return res.getLong("deployAppId");
        } catch (Exception e) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("launch remote unit deployment failed|unitId=%s|req=%s|exception=%s",
                            unitId, JSONObject.toJSONString(req), ExceptionUtils.getStackTrace(e)));
        }
    }
}
