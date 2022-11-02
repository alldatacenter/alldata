package com.alibaba.tesla.appmanager.trait.plugin;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.domain.core.WorkloadResource;
import com.alibaba.tesla.appmanager.domain.schema.TraitDefinition;
import com.alibaba.tesla.appmanager.trait.BaseTrait;
import lombok.extern.slf4j.Slf4j;

/**
 * Pod Security 设置 Trait
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class PodSecurityContextTrait extends BaseTrait {

    public PodSecurityContextTrait(String name, TraitDefinition traitDefinition, JSONObject spec, WorkloadResource ref) {
        super(name, traitDefinition, spec, ref);
    }

    @Override
    public void execute() {
        JSONObject spec = getSpec();
        if (spec == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "invalid pod security context trait spec");
        }
        JSONObject workloadSpec = (JSONObject) getWorkloadRef().getSpec();

        // 适配 cloneset 及 advancedstatefulset 类型
        if (workloadSpec.get("cloneSet") != null) {
            JSONObject cloneSetSpec = workloadSpec
                    .getJSONObject("cloneSet")
                    .getJSONObject("template")
                    .getJSONObject("spec");
            cloneSetSpec.putIfAbsent("securityContext", new JSONObject());
            cloneSetSpec.getJSONObject("securityContext").putAll(spec);
        } else if (workloadSpec.get("advancedStatefulSet") != null) {
            JSONObject advancedStatefulSetSpec = workloadSpec
                    .getJSONObject("advancedStatefulSet")
                    .getJSONObject("template")
                    .getJSONObject("spec");
            advancedStatefulSetSpec.putIfAbsent("securityContext", new JSONObject());
            advancedStatefulSetSpec.getJSONObject("securityContext").putAll(spec);
        } else {
            throw new AppException("pod security context trait cannot only apply to CloneSet/AdvancedStatefulSet");
        }
        log.info("pod security context trait has applied to workload {}|spec={}",
                JSONObject.toJSONString(getWorkloadRef().getMetadata()), spec.toJSONString());
    }
}
