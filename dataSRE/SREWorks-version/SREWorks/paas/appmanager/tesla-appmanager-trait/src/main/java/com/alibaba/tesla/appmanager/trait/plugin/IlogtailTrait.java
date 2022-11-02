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
 * ilogtail Trait，用于和阿里云 SLS 交互
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class IlogtailTrait extends BaseTrait {

    /**
     * ilogtail container string
     */
    private static final String ILOGTAIL_CONTAINER = "{\n" +
            "    \"name\": \"logtail\",\n" +
            "    \"image\": \"reg.docker.alibaba-inc.com/abm-aone/logtail:v1.0.27.0-7052198-aliyun\",\n" +
            "    \"imagePullPolicy\": \"IfNotPresent\",\n" +
            "    \"command\": [\n" +
            "        \"sh\",\n" +
            "        \"-c\",\n" +
            "        \"/usr/local/ilogtail/run_logtail.sh 10\"\n" +
            "    ],\n" +
            "    \"livenessProbe\": {\n" +
            "        \"exec\": {\n" +
            "            \"command\": [\n" +
            "                \"/etc/init.d/ilogtaild\",\n" +
            "                \"status\"\n" +
            "            ]\n" +
            "        },\n" +
            "        \"initialDelaySeconds\": 30,\n" +
            "        \"periodSeconds\": 30\n" +
            "    },\n" +
            "    \"resources\": {\n" +
            "        \"requests\": {\n" +
            "            \"memory\": \"10Mi\",\n" +
            "            \"cpu\": \"30m\"\n" +
            "        },\n" +
            "        \"limits\": {\n" +
            "            \"memory\": \"512Mi\",\n" +
            "            \"cpu\": \"100m\"\n" +
            "        }\n" +
            "    },\n" +
            "    \"env\": [\n" +
            "        {\n" +
            "            \"name\": \"ALIYUN_LOGTAIL_USER_ID\",\n" +
            "            \"value\": \"%s\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"SIGMA_IGNORE_RESOURCE\",\n" +
            "            \"value\": \"true\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"ALIYUN_LOGTAIL_USER_DEFINED_ID\",\n" +
            "            \"value\": \"%s\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"ALIYUN_LOGTAIL_CONFIG\",\n" +
            "            \"value\": \"/etc/ilogtail/conf/%s/ilogtail_config.json\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"ALIYUN_LOG_ENV_TAGS\",\n" +
            "            \"value\": \"_pod_name_|_pod_ip_|_namespace_|_node_name_|_node_ip_\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"_pod_name_\",\n" +
            "            \"valueFrom\": {\n" +
            "                \"fieldRef\": {\n" +
            "                    \"fieldPath\": \"metadata.name\"\n" +
            "                }\n" +
            "            }\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"_pod_ip_\",\n" +
            "            \"valueFrom\": {\n" +
            "                \"fieldRef\": {\n" +
            "                    \"fieldPath\": \"status.podIP\"\n" +
            "                }\n" +
            "            }\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"_namespace_\",\n" +
            "            \"valueFrom\": {\n" +
            "                \"fieldRef\": {\n" +
            "                    \"fieldPath\": \"metadata.namespace\"\n" +
            "                }\n" +
            "            }\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"_node_name_\",\n" +
            "            \"valueFrom\": {\n" +
            "                \"fieldRef\": {\n" +
            "                    \"fieldPath\": \"spec.nodeName\"\n" +
            "                }\n" +
            "            }\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"_node_ip_\",\n" +
            "            \"valueFrom\": {\n" +
            "                \"fieldRef\": {\n" +
            "                    \"fieldPath\": \"status.hostIP\"\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "    ],\n" +
            "    \"volumeMounts\": []\n" +
            "}";

    /**
     * 初始化
     *  @param name            名称
     * @param traitDefinition Trait 定义
     * @param spec            当前 Trait Spec (options)
     * @param ref             当前 Trait 绑定的 workload ref 引用
     */
    public IlogtailTrait(String name, TraitDefinition traitDefinition, JSONObject spec, WorkloadResource ref) {
        super(name, traitDefinition, spec, ref);
    }

    @Override
    public void execute() {
        JSONObject spec = getSpec();
        String userId = spec.getString("userId");
        String userDefinedId = spec.getString("userDefinedId");
        String regionId = spec.getString("regionId");
        JSONArray volumeMounts = spec.getJSONArray("volumeMounts");
        if (StringUtils.isAnyEmpty(userId, userDefinedId, regionId) || volumeMounts == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    "userId/userDefinedId/regionId/volumeMounts are required");
        }

        // 组装 ilogtail container spec
        String containerSpecStr = String.format(ILOGTAIL_CONTAINER, userId, userDefinedId, regionId);
        JSONObject containerSpec = JSONObject.parseObject(containerSpecStr);
        containerSpec.put("volumeMounts", volumeMounts);

        // 放置到实际配置中
        JSONObject workloadSpec = (JSONObject) getWorkloadRef().getSpec();
        JSONArray containers;
        if (workloadSpec.get("cloneSet") != null) {
            containers = workloadSpec.getJSONObject("cloneSet")
                    .getJSONObject("template")
                    .getJSONObject("spec")
                    .getJSONArray("containers");
        } else if (workloadSpec.get("advancedStatefulSet") != null) {
            containers = workloadSpec.getJSONObject("advancedStatefulSet")
                    .getJSONObject("template")
                    .getJSONObject("spec")
                    .getJSONArray("containers");
        } else {
            containers = workloadSpec.getJSONArray("containers");
        }
        containers.add(containerSpec);
        log.info("ilogtail trait {} has applied to cloneset workload {}",
                containerSpecStr, JSONObject.toJSONString(getWorkloadRef().getMetadata()));
    }
}
