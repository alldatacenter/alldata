package com.alibaba.tesla.appmanager.trait.plugin;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.domain.core.WorkloadResource;
import com.alibaba.tesla.appmanager.domain.schema.TraitDefinition;
import com.alibaba.tesla.appmanager.trait.BaseTrait;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * 和宿主机同步时区 Trait
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class TimezoneSyncTrait extends BaseTrait {

    public static final String VOLUME_NAME = "timezone-sync-trait-volume-config";

    public TimezoneSyncTrait(String name, TraitDefinition traitDefinition, JSONObject spec, WorkloadResource ref) {
        super(name, traitDefinition, spec, ref);
    }

    @Override
    public void execute() {
        String timezone = getSpec().getString("timezone");
        if (StringUtils.isEmpty(timezone)) {
            timezone = "Asia/Shanghai";
        }
        JSONObject workloadSpec = (JSONObject) getWorkloadRef().getSpec();

        // 适配 cloneset 及 advancedstatefulset 类型，最后是兼容历史的类型，直接置到 workload spec 顶部
        if (workloadSpec.get("cloneSet") != null) {
            JSONObject cloneSetSpec = workloadSpec
                    .getJSONObject("cloneSet")
                    .getJSONObject("template")
                    .getJSONObject("spec");
            JSONArray containers = cloneSetSpec.getJSONArray("containers");
            JSONArray initContainers = cloneSetSpec.getJSONArray("initContainers");
            updateContainerVolumeMounts(containers);
            updateContainerVolumeMounts(initContainers);
            updateVolume(cloneSetSpec, timezone);
        } else if (workloadSpec.get("advancedStatefulSet") != null) {
            JSONObject advancedStatefulSetSpec = workloadSpec
                    .getJSONObject("advancedStatefulSet")
                    .getJSONObject("template")
                    .getJSONObject("spec");
            JSONArray containers = advancedStatefulSetSpec.getJSONArray("containers");
            JSONArray initContainers = advancedStatefulSetSpec.getJSONArray("initContainers");
            updateContainerVolumeMounts(containers);
            updateContainerVolumeMounts(initContainers);
            updateVolume(advancedStatefulSetSpec, timezone);
        } else {
            JSONArray containers = workloadSpec.getJSONArray("containers");
            JSONArray initContainers = workloadSpec.getJSONArray("initContainers");
            JSONObject env = workloadSpec.getJSONObject("env");
            if (env != null && StringUtils.isEmpty(env.getString("TZ"))) {
                env.put("TZ", timezone);
            }
            updateContainerVolumeMounts(containers);
            updateContainerVolumeMounts(initContainers);
            updateVolume(workloadSpec, timezone);
        }

        log.info("timezone {} has applied to workload {}",
            timezone, JSONObject.toJSONString(getWorkloadRef().getMetadata()));
    }

    private static void updateVolume(JSONObject spec, String timezone) {
        final String KEY_VOLUME = "volumes";
        JSONArray volumes = spec.getJSONArray(KEY_VOLUME);
        if (volumes == null) {
            spec.put(KEY_VOLUME, new JSONArray());
            volumes = spec.getJSONArray(KEY_VOLUME);
        }
        volumes.add(getVolume(timezone));
    }

    private static void updateContainerVolumeMounts(JSONArray containers) {
        final String KEY_VOLUME_MOUNTS = "volumeMounts";
        if (containers != null) {
            for (int i = 0; i < containers.size(); i++) {
                JSONObject container = containers.getJSONObject(i);
                JSONArray volumeMounts = container.getJSONArray(KEY_VOLUME_MOUNTS);
                if (volumeMounts == null) {
                    container.put(KEY_VOLUME_MOUNTS, new JSONArray());
                    volumeMounts = container.getJSONArray(KEY_VOLUME_MOUNTS);
                }
                volumeMounts.add(getContainerVolumeMounts());
            }
        }
    }

    private static JSONObject getVolume(String timezone) {
        JSONObject obj = new JSONObject();
        obj.put("name", VOLUME_NAME);
        obj.put("hostPath", new JSONObject());
        obj.getJSONObject("hostPath").put("path", String.format("/usr/share/zoneinfo/%s", timezone));
        obj.getJSONObject("hostPath").put("type", "File");
        return obj;
    }

    private static JSONObject getContainerVolumeMounts() {
        JSONObject obj = new JSONObject();
        obj.put("name", VOLUME_NAME);
        obj.put("mountPath", "/etc/localtime");
        return obj;
    }
}
