package com.alibaba.tesla.appmanager.trait.plugin;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.domain.core.WorkloadResource;
import com.alibaba.tesla.appmanager.domain.schema.TraitDefinition;
import com.alibaba.tesla.appmanager.trait.BaseTrait;
import lombok.extern.slf4j.Slf4j;

/**
 * 附加环境变量 Trait
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class AdditionalEnvTrait extends BaseTrait {

    public AdditionalEnvTrait(String name, TraitDefinition traitDefinition, JSONObject spec, WorkloadResource ref) {
        super(name, traitDefinition, spec, ref);
    }

    @Override
    public void execute() {
        JSONObject workloadSpec = (JSONObject) getWorkloadRef().getSpec();
        JSONArray env = getSpec().getJSONArray("env");
        if (env == null || env.size() == 0) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "empty additional env spec configuration");
        }
        JSONArray containers = workloadSpec.getJSONArray("containers");
        appendContainerEnv(containers, env);
        log.info("addonitional env {} has applied to workload {}",
            JSONArray.toJSONString(env), JSONObject.toJSONString(getWorkloadRef().getMetadata()));
    }

    private static void appendContainerEnv(JSONArray containers, JSONArray env) {
        if (containers != null) {
            for (int i = 0; i < containers.size(); i++) {
                JSONObject container = containers.getJSONObject(i);
                JSONArray containerEnv = container.getJSONArray("env");
                if (containerEnv == null || containerEnv.size() == 0) {
                    containerEnv = new JSONArray();
                    container.put("env", containerEnv);
                }
                containerEnv.addAll(env);
            }
        }
    }
}
