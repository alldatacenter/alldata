package dynamicscripts

import com.alibaba.fastjson.JSONObject
import com.alibaba.tesla.appmanager.common.enums.DynamicScriptKindEnum
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode
import com.alibaba.tesla.appmanager.common.exception.AppException
import com.alibaba.tesla.appmanager.domain.req.trait.TraitExecuteReq
import com.alibaba.tesla.appmanager.domain.res.trait.TraitExecuteRes
import com.alibaba.tesla.appmanager.trait.service.handler.TraitHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Node Selector Trait
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
class TraitNodeSelector implements TraitHandler {

    private static final Logger log = LoggerFactory.getLogger(TraitNodeSelector.class)

    /**
     * 当前内置 Handler 类型
     */
    public static final String KIND = DynamicScriptKindEnum.TRAIT.toString()

    /**
     * 当前内置 Handler 名称
     */
    public static final String NAME = "nodeSelector.trait.abm.io"

    /**
     * 当前内置 Handler 版本
     */
    public static final Integer REVISION = 0

    /**
     * Trait 业务侧逻辑执行
     *
     * @param request Trait 输入参数
     * @return Trait 修改后的 Spec 定义
     */
    @Override
    TraitExecuteRes execute(TraitExecuteReq request) {
        def spec = request.getSpec()
        def nodeSelector = spec.getJSONObject("nodeSelector")
        if (nodeSelector == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "empty nodeSelector trait spec")
        }

        JSONObject workloadSpec = (JSONObject) request.getRef().getSpec()

        // 适配 cloneset 及 advancedstatefulset 类型，最后是兼容历史的类新，直接置到 workload spec 顶部
        if (workloadSpec.get("cloneSet") != null) {
            JSONObject cloneSetSpec = workloadSpec
                    .getJSONObject("cloneSet")
                    .getJSONObject("template")
                    .getJSONObject("spec")
            if (cloneSetSpec.get("nodeSelector") == null) {
                cloneSetSpec.put("nodeSelector", nodeSelector)
                log.info("nodeSelector {} has applied to workload {}|kind=cloneSet|append=false",
                        nodeSelector.toJSONString(), JSONObject.toJSONString(getWorkloadRef().getMetadata()))
            } else {
                cloneSetSpec.getJSONObject("nodeSelector").putAll(nodeSelector)
                log.info("nodeSelector {} has applied to workload {}|kind=cloneSet|append=true",
                        nodeSelector.toJSONString(), JSONObject.toJSONString(request.getRef().getMetadata()))
            }
        } else if (workloadSpec.get("advancedStatefulSet") != null) {
            JSONObject advancedStatefulSetSpec = workloadSpec
                    .getJSONObject("advancedStatefulSet")
                    .getJSONObject("template")
                    .getJSONObject("spec")
            if (advancedStatefulSetSpec.get("nodeSelector") == null) {
                advancedStatefulSetSpec.put("nodeSelector", nodeSelector)
                log.info("nodeSelector {} has applied to workload {}|kind=advancedStatefulSet|append=false",
                        nodeSelector.toJSONString(), JSONObject.toJSONString(request.getRef().getMetadata()))
            } else {
                advancedStatefulSetSpec.getJSONObject("nodeSelector").putAll(nodeSelector)
                log.info("nodeSelector {} has applied to workload {}|kind=advancedStatefulSet|append=true",
                        nodeSelector.toJSONString(), JSONObject.toJSONString(request.getRef().getMetadata()))
            }
        } else {
            workloadSpec.put("nodeSelector", nodeSelector)
            log.info("nodeSelector {} has applied to workload {}|kind=compatible|append=false",
                    nodeSelector.toJSONString(), JSONObject.toJSONString(request.getRef().getMetadata()))
        }
        return TraitExecuteRes.builder()
                .spec(spec)
                .build()
    }
}
