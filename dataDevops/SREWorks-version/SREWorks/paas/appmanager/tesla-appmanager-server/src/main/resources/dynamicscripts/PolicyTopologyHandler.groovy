package dynamicscripts

import com.alibaba.tesla.appmanager.common.enums.DynamicScriptKindEnum
import com.alibaba.tesla.appmanager.domain.req.workflow.ExecutePolicyHandlerReq
import com.alibaba.tesla.appmanager.domain.res.workflow.ExecutePolicyHandlerRes
import com.alibaba.tesla.appmanager.workflow.dynamicscript.PolicyHandler
import lombok.extern.slf4j.Slf4j
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Policy Topology Handler
 *
 * Properties:
 *
 * * cluster
 * * namespace
 * * stage
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
class PolicyTopologyHandler implements PolicyHandler {

    private static final Logger log = LoggerFactory.getLogger(PolicyTopologyHandler.class)

    /**
     * 当前内置 Handler 类型
     */
    public static final String KIND = DynamicScriptKindEnum.POLICY.toString()

    /**
     * 当前内置 Handler 名称
     */
    public static final String NAME = "topology"

    /**
     * 当前内置 Handler 版本
     */
    public static final Integer REVISION = 0

    @Override
    ExecutePolicyHandlerRes execute(ExecutePolicyHandlerReq request) throws InterruptedException {
        def properties = request.getPolicyProperties()
        def cluster = properties.getString("cluster")
        def namespace = properties.getString("namespace")
        def stage = properties.getString("stage")
        def configuration = request.getConfiguration()
        def context = request.getContext()
        for (def component : configuration.getSpec().getComponents()) {
            if (StringUtils.isNotEmpty(cluster)) {
                component.setClusterId(cluster)
            }
            if (StringUtils.isNotEmpty(namespace)) {
                component.setNamespaceId(namespace)
            }
            if (StringUtils.isNotEmpty(stage)) {
                component.setStageId(stage)
            }
        }
        return ExecutePolicyHandlerRes.builder()
                .context(context)
                .configuration(configuration)
                .build()
    }
}
