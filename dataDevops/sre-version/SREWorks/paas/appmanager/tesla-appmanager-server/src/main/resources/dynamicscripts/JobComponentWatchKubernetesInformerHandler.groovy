package dynamicscripts

import com.alibaba.fastjson.JSONObject
import com.alibaba.tesla.appmanager.autoconfig.SystemProperties
import com.alibaba.tesla.appmanager.common.enums.ComponentInstanceStatusEnum
import com.alibaba.tesla.appmanager.domain.core.InstanceCondition
import com.alibaba.tesla.appmanager.domain.req.componentinstance.ReportRtComponentInstanceStatusReq
import com.alibaba.tesla.appmanager.server.dynamicscript.handler.ComponentWatchKubernetesInformerHandler
import com.alibaba.tesla.appmanager.server.service.rtcomponentinstance.RtComponentInstanceService
import io.fabric8.kubernetes.api.model.batch.v1.Job
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.informers.ResourceEventHandler
import io.fabric8.kubernetes.client.informers.SharedInformerFactory
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

/**
 * Job Watch Kubernetes Informer 处理器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
class JobComponentWatchKubernetesInformerHandler implements ComponentWatchKubernetesInformerHandler {

    private static final Logger log = LoggerFactory.getLogger(JobComponentWatchKubernetesInformerHandler.class)

    /**
     * Handler 元信息
     */
    public static final String KIND = "COMPONENT_WATCH_KUBERNETES_INFORMER"
    public static final String NAME = "JobInformerRegister"
    public static final Integer REVISION = 8

    /**
     * RESYNC 间隔时间
     */
    private static final long RESYNC_PERIOD_IN_MILLIS = 60 * 1000L

    /**
     * 当前 Groovy 对应的 Component 类型
     */
    private static final String COMPONENT_TYPE = "K8S_JOB"

    private static final String LABEL_APP_ID = "labels.appmanager.oam.dev/appId"
    private static final String LABEL_COMPONENT_NAME = "labels.appmanager.oam.dev/componentName"
    private static final String LABEL_CLUSTER = "labels.appmanager.oam.dev/clusterId"
    private static final String LABEL_STAGE_ID = "labels.appmanager.oam.dev/stageId"
    private static final String ANNOTATIONS_VERSION = "annotations.appmanager.oam.dev/version"
    private static final String ANNOTATIONS_COMPONENT_INSTANCE_ID = "annotations.appmanager.oam.dev/componentInstanceId"
    private static final String ANNOTATIONS_APP_INSTANCE_NAME = "annotations.appmanager.oam.dev/appInstanceName"

    @Autowired
    private RtComponentInstanceService componentInstanceService

    @Autowired
    private SystemProperties systemProperties

    /**
     * 集群维度的 informer 事件监听注册器 (监听 Job 对象变动)
     *
     * @param clusterId 集群 ID
     * @param namespaceId 限定 Namespace，可为 null 表示全局监听
     * @param client Kubernetes Client
     * @param sharedInformerFactory 初始化好的 SharedInformerFactory 对象
     */
    @Override
    void register(String clusterId, String namespaceId, DefaultKubernetesClient client,
                  SharedInformerFactory sharedInformerFactory) {
        def informer
        if (StringUtils.isNotEmpty(namespaceId)) {
            informer = sharedInformerFactory
                    .inNamespace(namespaceId)
                    .sharedIndexInformerFor(Job.class, RESYNC_PERIOD_IN_MILLIS)
        } else {
            informer = sharedInformerFactory
                    .sharedIndexInformerFor(Job.class, RESYNC_PERIOD_IN_MILLIS)
        }
        informer.addEventHandler(new ResourceEventHandler<Job>() {
            @Override
            void onAdd(Job cr) {
                update(cr)
            }

            @Override
            void onUpdate(Job cr, Job newCr) {
                update(newCr)
            }

            @Override
            void onDelete(Job cr, boolean deletedFinalStateUnknown) {
                update(cr)
            }

            /**
             * 根据传入的 CR 更新当前的组件状态
             * @param cr CR 对象
             */
            private void update(Job cr) {
                def labels = cr.getMetadata().getLabels()
                def annotations = cr.getMetadata().getAnnotations()
                def appId = labels.get(LABEL_APP_ID)
                def componentName = labels.get(LABEL_COMPONENT_NAME)
                def actualClusterId = labels.get(LABEL_CLUSTER)
                def stageId = labels.get(LABEL_STAGE_ID)
                if (annotations == null || annotations.size() == 0) {
                    log.debug("invalid component instance found, skip|appId={}|componentName={}|stageId={}|labels={}",
                            appId, componentName, stageId, JSONObject.toJSONString(labels))
                    return
                } else if (actualClusterId != clusterId) {
                    return
                }

                def version = annotations.getOrDefault(ANNOTATIONS_VERSION, "")
                def componentInstanceId = annotations.getOrDefault(ANNOTATIONS_COMPONENT_INSTANCE_ID, "")
                def appInstanceName = annotations.getOrDefault(ANNOTATIONS_APP_INSTANCE_NAME, "")

                // 检测是否存在 componentInstanceId，如果不存在，则无法上报数据
                if (StringUtils.isEmpty(componentInstanceId)) {
                    log.debug("invalid component instance found, skip|appId={}|componentName={}|stageId={}|version={}",
                            appId, componentName, stageId, version)
                    return
                }

                // 状态获取及转换
                def finalConditions = new ArrayList<InstanceCondition>()
                def completed = false
                def failed = false
                cr.getStatus().getConditions().forEach({ c ->
                    finalConditions.add(InstanceCondition.builder()
                            .type(c.getType())
                            .status(c.getStatus())
                            .reason(c.getReason())
                            .message(JSONObject.toJSONString(c))
                            .build())
                    if (c.getType() == "Complete" && c.getStatus() == "True") {
                        completed = true
                    } else if (c.getType() == "Failed" && c.getStatus() == "True") {
                        failed = true
                    }
                })
                def finalStatus
                if (completed) {
                    finalStatus = ComponentInstanceStatusEnum.COMPLETED
                } else if (failed) {
                    finalStatus = ComponentInstanceStatusEnum.FAILED
                } else {
                    finalStatus = ComponentInstanceStatusEnum.RUNNING
                }

                // 上报状态
                componentInstanceService.report(ReportRtComponentInstanceStatusReq.builder()
                        .componentInstanceId(componentInstanceId)
                        .appInstanceName(appInstanceName)
                        .clusterId(clusterId)
                        .namespaceId(cr.getMetadata().getNamespace())
                        .stageId(stageId)
                        .appId(appId)
                        .componentType(COMPONENT_TYPE)
                        .componentName(componentName)
                        .version(version)
                        .status(finalStatus.toString())
                        .conditions(finalConditions)
                        .build())
            }
        })
    }

    /**
     * 是否限制到指定的 namespace 中
     *
     * @return namespace 名称
     */
    @Override
    String restrictNamespace() {
        return systemProperties.getRestrictNamespace()
    }
}
