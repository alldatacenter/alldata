package com.alibaba.tesla.appmanager.trait.plugin;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.domain.core.WorkloadResource;
import com.alibaba.tesla.appmanager.domain.schema.TraitDefinition;
import com.alibaba.tesla.appmanager.trait.BaseTrait;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * 改变 Service Account Name
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class ServiceAccountNameTrait extends BaseTrait {

    public ServiceAccountNameTrait(String name, TraitDefinition traitDefinition, JSONObject spec, WorkloadResource ref) {
        super(name, traitDefinition, spec, ref);
    }

    @Override
    public void execute() {
        String serviceAccountName = getSpec().getString("serviceAccountName");
        if (StringUtils.isEmpty(serviceAccountName)) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "serviceAccountName is required in trait");
        }

        JSONObject workloadSpec = (JSONObject) getWorkloadRef().getSpec();

        // 适配 cloneset 及 advancedstatefulset 类型
        if (workloadSpec.get("cloneSet") != null) {
            JSONObject cloneSetSpec = workloadSpec
                    .getJSONObject("cloneSet")
                    .getJSONObject("template")
                    .getJSONObject("spec");
            cloneSetSpec.put("serviceAccountName", serviceAccountName);
        } else if (workloadSpec.get("advancedStatefulSet") != null) {
            JSONObject advancedStatefulSetSpec = workloadSpec
                    .getJSONObject("advancedStatefulSet")
                    .getJSONObject("template")
                    .getJSONObject("spec");
            advancedStatefulSetSpec.put("serviceAccountName", serviceAccountName);
        } else {
            throw new AppException("serviceAccountName trait cannot only apply to CloneSet/AdvancedStatefulSet");
        }
        log.info("serviceAccountName trait has applied to workload {}|serviceAccountName={}",
                JSONObject.toJSONString(getWorkloadRef().getMetadata()), serviceAccountName);
    }
}
