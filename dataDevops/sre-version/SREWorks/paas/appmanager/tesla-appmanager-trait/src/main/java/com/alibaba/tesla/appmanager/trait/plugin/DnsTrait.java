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
 * DNS Trait
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class DnsTrait extends BaseTrait {

    public DnsTrait(String name, TraitDefinition traitDefinition, JSONObject spec, WorkloadResource ref) {
        super(name, traitDefinition, spec, ref);
    }

    @Override
    public void execute() {
        String dnsPolicy = getSpec().getString("dnsPolicy");
        if (StringUtils.isEmpty(dnsPolicy)) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "dnsPlicy is required in dns trait");
        }

        JSONObject dnsConfig = getSpec().getJSONObject("dnsConfig");
        JSONObject workloadSpec = (JSONObject) getWorkloadRef().getSpec();

        // 适配 cloneset 及 advancedstatefulset 类型
        if (workloadSpec.get("cloneSet") != null) {
            JSONObject cloneSetSpec = workloadSpec
                .getJSONObject("cloneSet")
                .getJSONObject("template")
                .getJSONObject("spec");
            cloneSetSpec.put("dnsPolicy", dnsPolicy);
            if (dnsConfig != null) {
                cloneSetSpec.put("dnsConfig", dnsConfig);
            }
        } else if (workloadSpec.get("advancedStatefulSet") != null) {
            JSONObject advancedStatefulSetSpec = workloadSpec
                .getJSONObject("advancedStatefulSet")
                .getJSONObject("template")
                .getJSONObject("spec");
            advancedStatefulSetSpec.put("dnsPolicy", dnsPolicy);
            if (dnsConfig != null) {
                advancedStatefulSetSpec.put("dnsConfig", dnsConfig);
            }
        } else {
            throw new AppException("dns trait cannot only apply to CloneSet/AdvancedStatefulSet");
        }
        log.info("dns trait has applied to workload {}|dnsPolicy={}|dnsConfig={}",
            JSONObject.toJSONString(getWorkloadRef().getMetadata()),
            dnsPolicy, dnsConfig != null ? JSONObject.toJSONString(dnsConfig) : "");
    }
}
