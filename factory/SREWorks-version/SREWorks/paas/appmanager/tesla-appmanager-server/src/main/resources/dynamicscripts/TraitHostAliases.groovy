package dynamicscripts

import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import com.alibaba.tesla.appmanager.common.enums.DynamicScriptKindEnum
import com.alibaba.tesla.appmanager.common.exception.AppException
import com.alibaba.tesla.appmanager.domain.req.trait.TraitExecuteReq
import com.alibaba.tesla.appmanager.domain.res.trait.TraitExecuteRes
import com.alibaba.tesla.appmanager.trait.service.handler.TraitHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Host Alias Trait
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
class TraitHostAliases implements TraitHandler {

    private static final Logger log = LoggerFactory.getLogger(TraitHostAliases.class)

    /**
     * 当前内置 Handler 类型
     */
    public static final String KIND = DynamicScriptKindEnum.TRAIT.toString()

    /**
     * 当前内置 Handler 名称
     */
    public static final String NAME = "hostAliases.trait.abm.io"

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
        def hostAliases = request.getSpec().getJSONArray("hostAliases")
        if (hostAliases == null) {
            return TraitExecuteRes.builder().spec(request.getSpec()).build()
        }

        def workloadSpec = ((JSONObject) request.getRef().getSpec())

        // 适配 cloneset 及 advancedstatefulset 类型
        if (workloadSpec.get("cloneSet") != null) {
            JSONObject cloneSetSpec = workloadSpec
                    .getJSONObject("cloneSet")
                    .getJSONObject("template")
                    .getJSONObject("spec")
            cloneSetSpec.putIfAbsent("hostAliases", new JSONArray())
            cloneSetSpec.getJSONArray("hostAliases").addAll(hostAliases)
        } else if (workloadSpec.get("advancedStatefulSet") != null) {
            JSONObject advancedStatefulSetSpec = workloadSpec
                    .getJSONObject("advancedStatefulSet")
                    .getJSONObject("template")
                    .getJSONObject("spec")
            advancedStatefulSetSpec.putIfAbsent("hostAliases", new JSONArray())
            advancedStatefulSetSpec.getJSONArray("hostAliases").addAll(hostAliases)
        } else {
            throw new AppException("host aliases trait can only apply to CloneSet/AdvancedStatefulSet")
        }
        log.info("pod security context trait has applied to workload {}|spec={}",
                JSONObject.toJSONString(request.getRef().getMetadata()), request.getSpec().toJSONString())
        return TraitExecuteRes.builder()
                .spec(request.getSpec())
                .build()
    }
}
