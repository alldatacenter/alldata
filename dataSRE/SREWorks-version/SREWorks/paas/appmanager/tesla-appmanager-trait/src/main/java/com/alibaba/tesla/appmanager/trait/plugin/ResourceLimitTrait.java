package com.alibaba.tesla.appmanager.trait.plugin;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.domain.core.WorkloadResource;
import com.alibaba.tesla.appmanager.domain.schema.TraitDefinition;
import com.alibaba.tesla.appmanager.trait.BaseTrait;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * 资源限制 Trait
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class ResourceLimitTrait extends BaseTrait {

    /**
     * initContainer 默认资源
     */
    private static String INIT_CONTAINER_RESOURCE = "{\n" +
            "    \"requests\": {\n" +
            "        \"memory\": \"128Mi\",\n" +
            "        \"cpu\": \"100m\"\n" +
            "    },\n" +
            "    \"limits\": {\n" +
            "        \"memory\": \"512Mi\",\n" +
            "        \"cpu\": \"200m\"\n" +
            "    }\n" +
            "}";

    public ResourceLimitTrait(String name, TraitDefinition traitDefinition, JSONObject spec, WorkloadResource ref) {
        super(name, traitDefinition, spec, ref);
    }

    @Override
    public void execute() {
        JSONObject resources = getSpec().getJSONObject("resources");
        if (resources == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "empty resource limit trait spec");
        }

        // 针对敏捷版环境，所有 CPU 除以 6 并向上取整
        if ("ApsaraStackAgility".equals(System.getenv("CLOUD_TYPE"))) {
            JSONObject limits = resources.getJSONObject("limits");
            if (limits != null) {
                String cpu = limits.getString("cpu");
                if (!StringUtils.isEmpty(cpu)) {
                    int cpuInt;
                    if (cpu.endsWith("m")) {
                        cpuInt = Integer.parseInt(cpu.substring(0, cpu.length() - 1));
                    } else {
                        cpuInt = Integer.parseInt(cpu) * 1000;
                    }
                    cpuInt /= 6;
                    limits.put("cpu", String.format("%dm", cpuInt));
                }
            }
        }

        JSONObject workloadSpec = (JSONObject) getWorkloadRef().getSpec();

        // 适配 cloneset 及 advancedstatefulset 类型，最后是兼容历史的类型，直接置到 workload spec 顶部
        if (workloadSpec.get("cloneSet") != null) {
            JSONObject cloneSetSpec = workloadSpec
                .getJSONObject("cloneSet")
                .getJSONObject("template")
                .getJSONObject("spec");
            JSONArray containers = cloneSetSpec.getJSONArray("containers");
            updateContainerResources(containers, resources);
            JSONArray initContainers = cloneSetSpec.getJSONArray("initContainers");
            updateContainerResources(initContainers, JSONObject.parseObject(INIT_CONTAINER_RESOURCE));
            log.info("resource limit {} has applied to cloneset workload {}",
                resources.toJSONString(), JSONObject.toJSONString(getWorkloadRef().getMetadata()));
        } else if (workloadSpec.get("advancedStatefulSet") != null) {
            JSONObject advancedStatefulSetSpec = workloadSpec
                .getJSONObject("advancedStatefulSet")
                .getJSONObject("template")
                .getJSONObject("spec");
            JSONArray containers = advancedStatefulSetSpec.getJSONArray("containers");
            updateContainerResources(containers, resources);
            JSONArray initContainers = advancedStatefulSetSpec.getJSONArray("initContainers");
            updateContainerResources(initContainers, JSONObject.parseObject(INIT_CONTAINER_RESOURCE));
            log.info("resource limit {} has applied to advanced statefulset workload {}",
                resources.toJSONString(), JSONObject.toJSONString(getWorkloadRef().getMetadata()));
        } else {
            JSONArray containers = workloadSpec.getJSONArray("containers");
            updateContainerResources(containers, resources);
            JSONArray initContainers = workloadSpec.getJSONArray("initContainers");
            updateContainerResources(initContainers, JSONObject.parseObject(INIT_CONTAINER_RESOURCE));
            log.info("resource limit {} has applied to workload {}",
                resources.toJSONString(), JSONObject.toJSONString(getWorkloadRef().getMetadata()));
        }
    }

    private static void updateContainerResources(JSONArray containers, JSONObject resources) {
        if (containers != null) {
            for (int i = 0; i < containers.size(); i++) {
                JSONObject container = containers.getJSONObject(i);
                container.put("resources", resources);
            }
        }
    }
}
