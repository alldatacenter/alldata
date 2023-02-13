package dynamicscripts

import com.alibaba.fastjson.JSONObject
import com.alibaba.tesla.appmanager.common.enums.DynamicScriptKindEnum
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode
import com.alibaba.tesla.appmanager.common.exception.AppException
import com.alibaba.tesla.appmanager.common.util.SchemaUtil
import com.alibaba.tesla.appmanager.domain.req.deploy.DeployAppLaunchReq
import com.alibaba.tesla.appmanager.domain.req.workflow.ExecutePolicyHandlerReq
import com.alibaba.tesla.appmanager.domain.req.workflow.ExecuteWorkflowHandlerReq
import com.alibaba.tesla.appmanager.domain.res.deploy.DeployAppPackageLaunchRes
import com.alibaba.tesla.appmanager.domain.res.workflow.ExecuteWorkflowHandlerRes
import com.alibaba.tesla.appmanager.server.repository.condition.UnitQueryCondition
import com.alibaba.tesla.appmanager.server.repository.domain.UnitDO
import com.alibaba.tesla.appmanager.server.service.unit.UnitService
import com.alibaba.tesla.appmanager.workflow.dynamicscript.WorkflowHandler
import com.alibaba.tesla.appmanager.workflow.util.WorkflowHandlerUtil
import lombok.extern.slf4j.Slf4j
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
/**
 * Workflow Remote Deploy Handler
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
class WorkflowRemoteDeployHandler implements WorkflowHandler {

    private static final Logger log = LoggerFactory.getLogger(WorkflowRemoteDeployHandler.class)

    /**
     * 当前内置 Handler 类型
     */
    public static final String KIND = DynamicScriptKindEnum.WORKFLOW.toString()

    /**
     * 当前内置 Handler 名称
     */
    public static final String NAME = "remoteDeploy"

    /**
     * 当前内置 Handler 版本
     */
    public static final Integer REVISION = 5

    @Autowired
    private UnitService unitService

    /**
     * 执行逻辑
     * @param request Workflow 执行请求
     * @return Workflow 执行结果
     */
    @Override
    ExecuteWorkflowHandlerRes execute(ExecuteWorkflowHandlerReq request) throws InterruptedException {
        def configuration = request.getConfiguration()
        def context = request.getContext()
        log.info("enter workflow remote deploy task|workflowInstanceId={}|workflowTaskId={}|context={}",
                request.getInstanceId(), request.getTaskId(), JSONObject.toJSONString(context))

        // 如果已经由前置节点成功发起，那么直接进入等待状态
        if (context.getLongValue("deployAppId") > 0) {
            log.info("deploy request has applied (direct)|workflowInstanceId={}|workflowTaskId={}|appId={}|" +
                    "context={}|configuration={}", request.getInstanceId(), request.getTaskId(), request.getAppId(),
                    JSONObject.toJSONString(context), JSONObject.toJSONString(configuration))
            return ExecuteWorkflowHandlerRes.builder()
                    .deployAppId(context.getLongValue("deployAppId"))
                    .deployAppUnitId(context.getString("unitId"))
                    .deployAppNamespaceId(context.getString("namespaceId"))
                    .deployAppStageId(context.getString("stageId"))
                    .context(context)
                    .configuration(configuration)
                    .build()
        }

        def annotations = configuration.getMetadata().getAnnotations()
        def unitId = annotations.getUnitId()
        def namespaceId = annotations.getNamespaceId()
        def stageId = annotations.getStageId()
        def appPackageId = annotations.getAppPackageId()
        if (StringUtils.isEmpty(unitId)) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "invalid workflow unitId properties")
        }
        def policies = request.getTaskProperties().getJSONArray("policies")
        if (policies != null && policies.size() > 0) {
            for (def policyName : policies.toJavaList(String.class)) {
                def policy = WorkflowHandlerUtil.getPolicy(configuration, policyName)
                def policyHandler = WorkflowHandlerUtil.getPolicyHandler(policy.getType())
                def policyProperties = policy.getProperties()
                def req = ExecutePolicyHandlerReq.builder()
                        .appId(request.getAppId())
                        .instanceId(request.getInstanceId())
                        .taskId(request.getTaskId())
                        .policyProperties(policyProperties)
                        .context(context)
                        .configuration(configuration)
                        .build()
                log.info("preapre to execute policy in workflow task|workflowInstanceId={}|workflowTaskId={}|" +
                        "appId={}|context={}|configuration={}", request.getInstanceId(), request.getTaskId(),
                        request.getAppId(), JSONObject.toJSONString(context), JSONObject.toJSONString(configuration))
                def res = policyHandler.execute(req)
                if (res.getContext() != null) {
                    context = res.getContext()
                }
                if (res.getConfiguration() != null) {
                    configuration = res.getConfiguration()
                }
                log.info("policy has exeucted in workflow task|workflowInstanceId={}|workflowTaskId={}|appId={}|" +
                        "context={}|configuration={}", request.getInstanceId(), request.getTaskId(), request.getAppId(),
                        JSONObject.toJSONString(context), JSONObject.toJSONString(configuration))
            }
        }

        // 再次同步包到远端
        UnitDO unitDO = unitService.get(UnitQueryCondition.builder().unitId(unitId).build())
        if (unitDO == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "cannot find unit in database")
        }
        try {
            def syncResponse = unitService.syncRemote(unitId, appPackageId)
            def syncResponseData = syncResponse.getJSONObject("data")
            if (syncResponseData == null) {
                throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                        String.format("sync to remote unit failed, syncResponse=%s", syncResponse.toJSONString()))
            }
            appPackageId = syncResponseData.getLongValue("id")
            if (appPackageId == 0) {
                throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                        String.format("sync to remote unit failed, syncResponse=%s", syncResponse.toJSONString()))
            }
            configuration.getMetadata().getAnnotations().setUnitId("")
            configuration.getMetadata().getAnnotations().setAppPackageId(appPackageId)
            def launchResponse = unitService.launchDeployment(unitId, DeployAppLaunchReq.builder()
                    .appPackageId(appPackageId)
                    .configuration(SchemaUtil.toYamlMapStr(configuration))
                    .autoEnvironment("true")
                    .removeSuffix(false)
                    .build())
            def res = launchResponse.toJavaObject(DeployAppPackageLaunchRes.class)
            def deployAppId = res.getDeployAppId()
            log.info("deploy request has applied|workflowInstanceId={}|workflowTaskId={}|appId={}|context={}|" +
                    "configuration={}", request.getInstanceId(), request.getTaskId(), request.getAppId(),
                    JSONObject.toJSONString(context), JSONObject.toJSONString(configuration))
            return ExecuteWorkflowHandlerRes.builder()
                    .deployAppId(deployAppId)
                    .deployAppUnitId(unitId)
                    .deployAppNamespaceId(namespaceId)
                    .deployAppStageId(stageId)
                    .context(context)
                    .configuration(configuration)
                    .build()
        } catch (Exception e) {
            throw new AppException(AppErrorCode.NETWORK_ERROR,
                    String.format("cannot launch deployment in unit %s|appPackageId=%d|exception=%s",
                            unitId, appPackageId, ExceptionUtils.getStackTrace(e)))
        }
    }
}
