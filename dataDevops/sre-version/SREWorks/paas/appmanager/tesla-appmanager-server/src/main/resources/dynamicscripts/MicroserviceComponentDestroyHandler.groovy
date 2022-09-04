package dynamicscripts


import com.alibaba.tesla.appmanager.common.enums.DynamicScriptKindEnum
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode
import com.alibaba.tesla.appmanager.common.exception.AppException
import com.alibaba.tesla.appmanager.domain.req.destroy.DestroyComponentInstanceReq
import com.alibaba.tesla.appmanager.kubernetes.KubernetesClientFactory
import com.alibaba.tesla.appmanager.server.dynamicscript.handler.ComponentDestroyHandler
import com.alibaba.tesla.appmanager.server.repository.condition.RtAppInstanceQueryCondition
import com.alibaba.tesla.appmanager.server.service.rtappinstance.RtAppInstanceService
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

/**
 * Microservice 组件销毁 Handler
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
class MicroserviceComponentDestroyHandler implements ComponentDestroyHandler {

    private static final Logger log = LoggerFactory.getLogger(MicroserviceComponentDestroyHandler.class)

    /**
     * Handler 元信息
     */
    public static final String KIND = DynamicScriptKindEnum.COMPONENT_DESTROY.toString()
    public static final String NAME = "MicroserviceDefault"
    public static final Integer REVISION = 3

    /**
     * CRD Context
     */
    private static final CustomResourceDefinitionContext CRD_CONTEXT = new CustomResourceDefinitionContext.Builder()
            .withName("microservices.apps.abm.io")
            .withGroup("apps.abm.io")
            .withVersion("v1")
            .withPlural("microservices")
            .withScope("Namespaced")
            .build()

    @Autowired
    private KubernetesClientFactory kubernetesClientFactory

    @Autowired
    private RtAppInstanceService rtAppInstanceService

    /**
     * 销毁组件实例
     *
     * @param request 销毁请求
     */
    @Override
    void destroy(DestroyComponentInstanceReq request) {
        def appId = request.getAppId()
        def componentName = request.getComponentName()
        def namespace = request.getNamespaceId()
        def stageId = request.getStageId()
        def name = getMetaName(appId, componentName, stageId)
        def client = kubernetesClientFactory.get(request.getClusterId())
        def logSuffix = String.format("appId=%s|componentName=%s|namespace=%s|stageId=%s|name=%s",
                appId, componentName, namespace, stageId, name)

        // 确认是否存在 owner reference 配置
        def appInstance = rtAppInstanceService.get(RtAppInstanceQueryCondition.builder()
                .appInstanceId(request.getAppInstanceId())
                .build())
        if (appInstance == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "cannot find app instance when destroy component " +
                    "instance|{}", logSuffix)
        } else if (StringUtils.isNotEmpty(appInstance.getOwnerReference())) {
            log.info("find owner reference in app instance, no need to delete microservice cr, skip|{}", logSuffix)
            return
        }

        // 删除历史上创建出来的 CR/svc/ingress (兼容使用)
        def microserviceCr = client.customResource(CRD_CONTEXT).inNamespace(namespace).withName(name).get()
        if (microserviceCr == null || microserviceCr.size() == 0) {
            log.info("no need to delete microservice cr, skip|{}", logSuffix)
        } else {
            client.customResource(CRD_CONTEXT).inNamespace(namespace).withName(name).delete()
            log.info("delete microservice cr succeed|{}", logSuffix)
        }
        def svcCr = client.services().inNamespace(namespace).withName(name).get()
        if (svcCr == null) {
            log.info("no need to delete service, skip|{}", logSuffix)
        } else {
            client.services().inNamespace(namespace).withName(name).delete()
            log.info("delete service succeed|{}", logSuffix)
        }
        def ingressCr = client.extensions().ingresses().inNamespace(namespace).withName(name).get()
        if (ingressCr == null) {
            log.info("no need to delete ingress, skip|{}", logSuffix)
        } else {
            client.extensions().ingresses().inNamespace(namespace).withName(name).delete()
            log.info("delete ingress succeed|{}", logSuffix)
        }
    }

    private static String getMetaName(String appId, String componentName, String stageId) {
        def name
        if (StringUtils.isEmpty(stageId)) {
            name = String.format("%s-%s", appId, componentName)
        } else {
            name = String.format("%s-%s-%s", stageId, appId, componentName)
        }
        return name
    }
}
