package com.alibaba.tesla.appmanager.trait.plugin;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.domain.core.WorkloadResource;
import com.alibaba.tesla.appmanager.domain.schema.TraitDefinition;
import com.alibaba.tesla.appmanager.trait.BaseTrait;
import lombok.extern.slf4j.Slf4j;

/**
 * 特权 Trait
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class PrivilegedTrait extends BaseTrait {

    public PrivilegedTrait(String name, TraitDefinition traitDefinition, JSONObject spec, WorkloadResource ref) {
        super(name, traitDefinition, spec, ref);
    }

    @Override
    public void execute() {
        JSONObject workloadSpec = (JSONObject) getWorkloadRef().getSpec();
        JSONArray containers = workloadSpec.getJSONArray("containers");
        updateContainerSecurityContext(containers);
        log.info("privileged security context has applied to workload {}",
            JSONObject.toJSONString(getWorkloadRef().getMetadata()));
    }

    private static void updateContainerSecurityContext(JSONArray containers) {
        JSONObject securityContext = new JSONObject();
        securityContext.put("privileged", true);
        if (containers != null) {
            for (int i = 0; i < containers.size(); i++) {
                JSONObject container = containers.getJSONObject(i);
                container.put("securityContext", securityContext);
            }
        }
    }
}
