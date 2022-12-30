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
 * 多集群部署亲和性 Trait
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class MultipleClusterAffinityTrait extends BaseTrait {

    public MultipleClusterAffinityTrait(String name, TraitDefinition traitDefinition, JSONObject spec, WorkloadResource ref) {
        super(name, traitDefinition, spec, ref);
    }

    @Override
    public void execute() {
        JSONObject spec = getSpec();
        String cluster = spec.getString("cluster");
        if (StringUtils.isEmpty(cluster)) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                "empty cluster in multiple cluster affinity trait spec");
        }
        JSONObject affinityValues = spec.getJSONObject(cluster).getJSONObject("affinity");
        JSONObject annotationValues = spec.getJSONObject(cluster).getJSONObject("annotations");
        JSONObject workloadSpec = (JSONObject) getWorkloadRef().getSpec();
        WorkloadResource.MetaData workloadMetadata = getWorkloadRef().getMetadata();
        if (affinityValues != null) {
            workloadSpec.put("affinity", affinityValues);
        }
        if (annotationValues != null) {
            ((JSONObject) workloadMetadata.getAnnotations()).putAll(annotationValues);
        }
        log.info("multiple cluster affinity trait has applied to workload {}",
            JSONObject.toJSONString(getWorkloadRef().getMetadata()));
    }
}
