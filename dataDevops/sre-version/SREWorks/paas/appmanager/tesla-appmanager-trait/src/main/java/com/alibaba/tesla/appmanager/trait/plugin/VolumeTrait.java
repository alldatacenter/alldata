package com.alibaba.tesla.appmanager.trait.plugin;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.RequestUtil;
import com.alibaba.tesla.appmanager.domain.core.WorkloadResource;
import com.alibaba.tesla.appmanager.domain.schema.TraitDefinition;
import com.alibaba.tesla.appmanager.trait.BaseTrait;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * Volume Trait
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class VolumeTrait extends BaseTrait {

    public VolumeTrait(String name, TraitDefinition traitDefinition, JSONObject spec, WorkloadResource ref) {
        super(name, traitDefinition, spec, ref);
    }

    @Override
    public void execute() {
        JSONObject spec = getSpec();
        JSONObject pvc = spec.getJSONObject("pvc");
        JSONArray volumeMounts = spec.getJSONArray("volumeMounts");
        JSONArray volumes = spec.getJSONArray("volumes");
        if (pvc == null || volumeMounts == null || volumes == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "invalid volume trait spec, empty configurations");
        }

        // 创建 PVC
        generatePvc(pvc.getJSONObject("metadata"), pvc.getJSONObject("spec"));

        // 修改 workload 中的 spec 定义，挂载申请出来的 pvc
        JSONObject workloadSpec = (JSONObject) getWorkloadRef().getSpec();
        JSONArray containers = workloadSpec.getJSONArray("containers");
        appendContainerSpec(containers, "volumeMounts", volumeMounts);

        // 增加 Volumes
        JSONArray originVolumes = workloadSpec.getJSONArray("volumes");
        if (originVolumes == null) {
            workloadSpec.put("volumes", new JSONArray());
            originVolumes = workloadSpec.getJSONArray("volumes");
        }
        originVolumes.addAll(volumes);
    }

    /**
     * 创建 PVC
     *
     * @param metadata 元信息定义
     * @param spec     spec 定义
     */
    private void generatePvc(JSONObject metadata, JSONObject spec) {
        if (metadata == null || spec == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    "invalid volume trait pvc configuration, empty metadata or spec");
        }
        String content = JSONObject.toJSONString(ImmutableMap.of(
            "yaml_content", ImmutableMap.of(
                "apiVersion", "v1",
                "kind", "PersistentVolumeClaim",
                "metadata", metadata,
                "spec", spec
            )
        ));
        JSONObject params = new JSONObject();
        params.put("overwrite", "true");
        try {
            String ret = RequestUtil.post("http://abm-operator/kube/apply", params, content, new JSONObject());
            log.info("apply pvc request has sent to abm-operator||content={}||response={}", content, ret);
            JSONObject retJson = JSONObject.parseObject(ret);
            // 暂时只打错误日志，error 是预期中的
            if (!"PersistentVolumeClaim".equals(retJson.getJSONObject("data").getString("kind"))) {
                log.error("cannot apply pvc to abm-operator||content={}||ret={}", content, ret);
            }
        } catch (Exception e) {
            throw new AppException(AppErrorCode.DEPLOY_ERROR,
                    String.format("cannot parse response from abm-operator||content=%s||exception=%s",
                            content, ExceptionUtils.getStackTrace(e)));
        }
    }

    /**
     * 更新 container 中指定 spec 中的 key 对应的内容
     *
     * @param containers 容器列表
     * @param key        Key
     * @param spec       映射内容
     */
    private static void appendContainerSpec(JSONArray containers, String key, JSONArray spec) {
        if (containers != null) {
            for (int i = 0; i < containers.size(); i++) {
                JSONObject container = containers.getJSONObject(i);
                JSONArray originValues = container.getJSONArray(key);
                if (originValues == null) {
                    container.put(key, new JSONArray());
                    originValues = container.getJSONArray(key);
                }
                originValues.addAll(spec);
            }
        }
    }
}
