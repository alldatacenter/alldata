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
 * Toleration Trait
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class TolerationTrait extends BaseTrait {

    public TolerationTrait(String name, TraitDefinition traitDefinition, JSONObject spec, WorkloadResource ref) {
        super(name, traitDefinition, spec, ref);
    }

    @Override
    public void execute() {
        JSONArray tolerations = getSpec().getJSONArray("tolerations");
        if (tolerations == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "empty toleration trait spec");
        }

        // 敏捷版环境增加 master 污点，并去除其他所有污点
        if ("ApsaraStackAgility".equals(System.getenv("CLOUD_TYPE"))) {
            // 增加敏捷版限制 label
            JSONObject labels = (JSONObject) getWorkloadRef().getMetadata().getLabels();
            labels.put("belong.foundation.alibabacloud.com", "true");
            // tolerations 清空
            tolerations = new JSONArray();
        }
        JSONObject workloadSpec = (JSONObject) getWorkloadRef().getSpec();

        // 适配 cloneset 及 advancedstatefulset 类型，最后是兼容历史的类新，直接置到 workload spec 顶部
        if (workloadSpec.get("cloneSet") != null) {
            JSONObject cloneSetSpec = workloadSpec
                    .getJSONObject("cloneSet")
                    .getJSONObject("template")
                    .getJSONObject("spec");
            if (cloneSetSpec.get("tolerations") == null) {
                cloneSetSpec.put("tolerations", tolerations);
                log.info("tolerations {} has applied to workload {}|kind=cloneSet|append=false",
                        tolerations.toJSONString(), JSONObject.toJSONString(getWorkloadRef().getMetadata()));
            } else {
                cloneSetSpec.getJSONArray("tolerations").addAll(tolerations);
                log.info("tolerations {} has applied to workload {}|kind=cloneSet|append=true",
                        tolerations.toJSONString(), JSONObject.toJSONString(getWorkloadRef().getMetadata()));
            }
        } else if (workloadSpec.get("advancedStatefulSet") != null) {
            JSONObject advancedStatefulSetSpec = workloadSpec
                    .getJSONObject("advancedStatefulSet")
                    .getJSONObject("template")
                    .getJSONObject("spec");
            if (advancedStatefulSetSpec.get("tolerations") == null) {
                advancedStatefulSetSpec.put("tolerations", tolerations);
                log.info("tolerations {} has applied to workload {}|kind=advancedStatefulSet|append=false",
                        tolerations.toJSONString(), JSONObject.toJSONString(getWorkloadRef().getMetadata()));
            } else {
                advancedStatefulSetSpec.getJSONArray("tolerations").addAll(tolerations);
                log.info("tolerations {} has applied to workload {}|kind=advancedStatefulSet|append=true",
                        tolerations.toJSONString(), JSONObject.toJSONString(getWorkloadRef().getMetadata()));
            }
        } else {
            workloadSpec.put("tolerations", tolerations);
            log.info("tolerations {} has applied to workload {}|kind=compatible|append=false",
                    tolerations.toJSONString(), JSONObject.toJSONString(getWorkloadRef().getMetadata()));
        }
    }
}
