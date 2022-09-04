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
public class JaegerAgentTrait extends BaseTrait {

    /**
     * ilogtail container string
     */
    private static final String JAEGER_AGENT_CONTAINER = "{\n" +
            "    \"name\": \"jaeger-agent\",\n" +
            "    \"image\": \"reg.docker.alibaba-inc.com/abm-aone/jaeger-agent:1.29\",\n" +
            "    \"imagePullPolicy\": \"IfNotPresent\",\n" +
            "    \"resources\": {\n" +
            "        \"requests\": {\n" +
            "            \"memory\": \"128Mi\",\n" +
            "            \"cpu\": \"100m\"\n" +
            "        },\n" +
            "        \"limits\": {\n" +
            "            \"memory\": \"512Mi\",\n" +
            "            \"cpu\": \"200m\"\n" +
            "        }\n" +
            "    },\n" +
            "    \"ports\": [\n" +
            "        {\n" +
            "            \"containerPort\": 5775,\n" +
            "            \"name\": \"zk-compact-trft\",\n" +
            "            \"protocol\": \"UDP\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"containerPort\": 5778,\n" +
            "            \"name\": \"config-rest\",\n" +
            "            \"protocol\": \"TCP\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"containerPort\": 6831,\n" +
            "            \"name\": \"jg-compact-trft\",\n" +
            "            \"protocol\": \"UDP\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"containerPort\": 6832,\n" +
            "            \"name\": \"jg-binary-trft\",\n" +
            "            \"protocol\": \"UDP\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"containerPort\": 14271,\n" +
            "            \"name\": \"admin-http\",\n" +
            "            \"protocol\": \"TCP\"\n" +
            "        }\n" +
            "    ],\n" +
            "    \"args\": [\n" +
            "        \"--reporter.grpc.host-port=%s\",\n" +
            "        \"--agent.tags=%s\"\n" +
            "    ],\n" +
            "    \"env\": [\n" +
            "        {\n" +
            "            \"name\": \"SIGMA_IGNORE_RESOURCE\",\n" +
            "            \"value\": \"true\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"JAEGER_SERVICE_NAME\",\n" +
            "            \"value\": \"%s\"\n" +
            "        }\n" +
            "    ]\n" +
            "}";

    /**
     * 初始化
     *  @param name            名称
     * @param traitDefinition Trait 定义
     * @param spec            当前 Trait Spec (options)
     * @param ref             当前 Trait 绑定的 workload ref 引用
     */
    public JaegerAgentTrait(String name, TraitDefinition traitDefinition, JSONObject spec, WorkloadResource ref) {
        super(name, traitDefinition, spec, ref);
    }

    @Override
    public void execute() {
        JSONObject spec = getSpec();
        String endpoint = spec.getString("endpoint");
        String tags = spec.getString("tags");
        String serviceName = spec.getString("serviceName");
        if (StringUtils.isAnyEmpty(endpoint, tags, serviceName)) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("endpoint/tags/serviceName are required|endpoint=%s|tags=%s|serviceName=%s|spec=%s",
                            endpoint, tags, serviceName, JSONObject.toJSONString(spec)));
        }

        // 组装 ilogtail container spec
        String containerSpecStr = String.format(JAEGER_AGENT_CONTAINER, endpoint, tags, serviceName);
        JSONObject containerSpec = JSONObject.parseObject(containerSpecStr);

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
        log.info("jaeger agent trait {} has applied to cloneset workload {}",
                containerSpecStr, JSONObject.toJSONString(getWorkloadRef().getMetadata()));
    }
}
