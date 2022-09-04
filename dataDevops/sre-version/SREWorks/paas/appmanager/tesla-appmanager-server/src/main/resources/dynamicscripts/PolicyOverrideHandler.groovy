package dynamicscripts

import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import com.alibaba.tesla.appmanager.common.enums.DynamicScriptKindEnum
import com.alibaba.tesla.appmanager.domain.container.DeployAppRevisionName
import com.alibaba.tesla.appmanager.domain.req.workflow.ExecutePolicyHandlerReq
import com.alibaba.tesla.appmanager.domain.res.workflow.ExecutePolicyHandlerRes
import com.alibaba.tesla.appmanager.domain.schema.DeployAppSchema
import com.alibaba.tesla.appmanager.workflow.dynamicscript.PolicyHandler
import lombok.extern.slf4j.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Policy Override Handler
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
class PolicyOverrideHandler implements PolicyHandler {

    private static final Logger log = LoggerFactory.getLogger(PolicyOverrideHandler.class)

    /**
     * 当前内置 Handler 类型
     */
    public static final String KIND = DynamicScriptKindEnum.POLICY.toString()

    /**
     * 当前内置 Handler 名称
     */
    public static final String NAME = "override"

    /**
     * 当前内置 Handler 版本
     */
    public static final Integer REVISION = 0

    @Override
    ExecutePolicyHandlerRes execute(ExecutePolicyHandlerReq request) throws InterruptedException {
        def overrideComponentProperties = request.getPolicyProperties().getJSONArray("components")
        def configuration = request.getConfiguration()
        for (def overrideComponent : overrideComponentProperties.toJavaList(JSONObject.class)) {
            def overrideRevisionName = DeployAppRevisionName.valueOf(overrideComponent.getString("revisionName"))
            for (def component : configuration.getSpec().getComponents()) {
                def revisionName = DeployAppRevisionName.valueOf(component.getRevisionName())
                if (overrideRevisionName.getComponentType() == revisionName.getComponentType()
                        && overrideRevisionName.getComponentName() == revisionName.getComponentName()) {
//                    enrichScopes(component, overrideComponent.getJSONArray("scopes"))
//                    enrichTraits(component, overrideComponent.getJSONArray("traits"))
//                    enrichDataInputs(component, overrideComponent.getJSONArray("dataInputs"))
//                    enrichDataOutputs(component, overrideComponent.getJSONArray("dataOutputs"))
//                    enrichDependencies(component, overrideComponent.getJSONArray("dependencies"))
                    enrichParameterValues(component, overrideComponent.getJSONArray("parameterValues"))
                }
            }
        }
        return ExecutePolicyHandlerRes.builder()
                .context(request.getContext())
                .configuration(configuration)
                .build()
    }

    private static void enrichParameterValues(DeployAppSchema.SpecComponent component, JSONArray data) {
        if (data == null || data.size() == 0) {
            return
        }
        if (component.getParameterValues() == null) {
            component.setParameterValues(new ArrayList<DeployAppSchema.ParameterValue>())
        }
        def overrideParameterValues = data.toJavaList(DeployAppSchema.ParameterValue)
        for (def overrideItem : overrideParameterValues) {
            boolean found = false
            for (def item : component.getParameterValues()) {
                if (item.getName() == overrideItem.getName()) {
                    found = true
                    item.setValue(overrideItem.getValue())
                    item.setToFieldPaths(overrideItem.getToFieldPaths())
                    break
                }
            }
            if (!found) {
                component.getParameterValues().add(overrideItem)
            }
        }
    }
}
