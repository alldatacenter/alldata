package com.alibaba.tesla.appmanager.trait.plugin;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.domain.core.WorkloadResource;
import com.alibaba.tesla.appmanager.domain.schema.TraitDefinition;
import com.alibaba.tesla.appmanager.trait.BaseTrait;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Skyline 打标 Trait
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class SkylineTrait extends BaseTrait {

    public SkylineTrait(String name, TraitDefinition traitDefinition, JSONObject spec, WorkloadResource ref) {
        super(name, traitDefinition, spec, ref);
    }

    @Override
    public void execute() {
        WorkloadResource.MetaData metadata = getWorkloadRef().getMetadata();
        String appStage = getSpec().getString("appStage");
        String appUnit = getSpec().getString("appUnit");
        String site = getSpec().getString("site");
        String appName = getSpec().getString("appName");
        String instanceGroup = getSpec().getString("instanceGroup");
        String namingRegisterState = getSpec().getString("namingRegisterState");
        assert !StringUtils.isEmpty(appStage);
        assert !StringUtils.isEmpty(appUnit);
        assert !StringUtils.isEmpty(site);
        assert !StringUtils.isEmpty(appName);
        assert !StringUtils.isEmpty(instanceGroup);
        assert !StringUtils.isEmpty(namingRegisterState);
        JSONObject labels = (JSONObject) metadata.getLabels();
        labels.put("pod.beta1.sigma.ali/naming-register-state", namingRegisterState);
        labels.put("sigma.ali/app-name", appName);
        labels.put("sigma.ali/site", site);
        labels.put("sigma.ali/instance-group", instanceGroup);
        labels.put("sigma.alibaba-inc.com/app-stage", appStage);
        labels.put("sigma.alibaba-inc.com/app-unit", appUnit);
//        JSONObject annotations = (JSONObject) metadata.getAnnotations();
//        annotations.put("pods.sigma.alibaba-inc.com/inject-pod-sn", "true");
        log.info("skyline trait has applied to workload {}|spec={}",
            JSONObject.toJSONString(getWorkloadRef().getMetadata()), JSONObject.toJSONString(getSpec()));
    }
}
