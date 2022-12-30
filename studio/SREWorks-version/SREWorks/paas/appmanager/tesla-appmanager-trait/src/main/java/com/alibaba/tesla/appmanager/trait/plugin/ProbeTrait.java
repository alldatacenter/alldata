package com.alibaba.tesla.appmanager.trait.plugin;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.domain.core.WorkloadResource;
import com.alibaba.tesla.appmanager.domain.schema.TraitDefinition;
import com.alibaba.tesla.appmanager.trait.BaseTrait;
import lombok.extern.slf4j.Slf4j;

/**
 * Probe Trait
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class ProbeTrait extends BaseTrait {

    public ProbeTrait(String name, TraitDefinition traitDefinition, JSONObject spec, WorkloadResource ref) {
        super(name, traitDefinition, spec, ref);
    }

    @Override
    public void execute() {
        JSONObject livenessProbe = getSpec().getJSONObject("livenessProbe");
        JSONObject startupProbe = getSpec().getJSONObject("startupProbe");
        JSONObject readinessProbe = getSpec().getJSONObject("readinessProbe");
        JSONObject workloadSpec = (JSONObject) getWorkloadRef().getSpec();

        // 适配 cloneset 及 advancedstatefulset 类型，最后是兼容历史的类型，直接置到 workload spec 顶部
        if (workloadSpec.get("cloneSet") != null) {
            JSONObject cloneSetSpec = workloadSpec
                .getJSONObject("cloneSet")
                .getJSONObject("template")
                .getJSONObject("spec");
            JSONArray containers = cloneSetSpec.getJSONArray("containers");
            if (livenessProbe != null) {
                updateContainerSpec(containers, "livenessProbe", livenessProbe);
            }
            if (startupProbe != null) {
                updateContainerSpec(containers, "startupProbe", startupProbe);
            }
            if (readinessProbe != null) {
                updateContainerSpec(containers, "readinessProbe", readinessProbe);
            }
            log.info("probe trait has applied to cloneset {}||livenessProbe={}||startupProbe={}||readinessProbe={}",
                JSONObject.toJSONString(getWorkloadRef().getMetadata()),
                livenessProbe != null ? JSONObject.toJSONString(livenessProbe) : "",
                startupProbe != null ? JSONObject.toJSONString(startupProbe) : "",
                readinessProbe != null ? JSONObject.toJSONString(readinessProbe) : "");
        } else if (workloadSpec.get("advancedStatefulSet") != null) {
            JSONObject advancedStatefulSetSpec = workloadSpec
                .getJSONObject("advancedStatefulSet")
                .getJSONObject("template")
                .getJSONObject("spec");
            JSONArray containers = advancedStatefulSetSpec.getJSONArray("containers");
            if (livenessProbe != null) {
                updateContainerSpec(containers, "livenessProbe", livenessProbe);
            }
            if (startupProbe != null) {
                updateContainerSpec(containers, "startupProbe", startupProbe);
            }
            if (readinessProbe != null) {
                updateContainerSpec(containers, "readinessProbe", readinessProbe);
            }
            log.info("probe trait has applied to advanced statefulset {}||livenessProbe={}||startupProbe={}||" +
                    "readinessProbe={}", JSONObject.toJSONString(getWorkloadRef().getMetadata()),
                livenessProbe != null ? JSONObject.toJSONString(livenessProbe) : "",
                startupProbe != null ? JSONObject.toJSONString(startupProbe) : "",
                readinessProbe != null ? JSONObject.toJSONString(readinessProbe) : "");
        } else {
            JSONArray containers = workloadSpec.getJSONArray("containers");
            if (livenessProbe != null) {
                if ("ApsaraStackAgility".equals(System.getenv("CLOUD_TYPE"))) {
                    Integer failureThreshold = livenessProbe.getInteger("failureThreshold");
                    if (failureThreshold != null && failureThreshold > 0) {
                        livenessProbe.put("failureThreshold", failureThreshold * 6);
                    }
                }
                updateContainerSpec(containers, "livenessProbe", livenessProbe);
            }
            if (startupProbe != null) {
                if ("ApsaraStackAgility".equals(System.getenv("CLOUD_TYPE"))) {
                    Integer failureThreshold = startupProbe.getInteger("failureThreshold");
                    if (failureThreshold != null && failureThreshold > 0) {
                        startupProbe.put("failureThreshold", failureThreshold * 6);
                    }
                }
                updateContainerSpec(containers, "startupProbe", startupProbe);
            }
            if (readinessProbe != null) {
                if ("ApsaraStackAgility".equals(System.getenv("CLOUD_TYPE"))) {
                    Integer failureThreshold = readinessProbe.getInteger("failureThreshold");
                    if (failureThreshold != null && failureThreshold > 0) {
                        readinessProbe.put("failureThreshold", failureThreshold * 6);
                    }
                }
                updateContainerSpec(containers, "readinessProbe", readinessProbe);
            }
            log.info("probe trait has applied to workload {}||livenessProbe={}||startupProbe={}||readinessProbe={}",
                JSONObject.toJSONString(getWorkloadRef().getMetadata()),
                livenessProbe != null ? JSONObject.toJSONString(livenessProbe) : "",
                startupProbe != null ? JSONObject.toJSONString(startupProbe) : "",
                readinessProbe != null ? JSONObject.toJSONString(readinessProbe) : "");
        }
    }

    private static void updateContainerSpec(JSONArray containers, String key, JSONObject spec) {
        if (containers != null) {
            for (int i = 0; i < containers.size(); i++) {
                JSONObject container = containers.getJSONObject(i);
                container.put(key, spec);
            }
        }
    }
}
