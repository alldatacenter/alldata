package com.alibaba.tesla.appmanager.server.addon.resources;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.common.util.AddonUtil;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.domain.schema.ComponentSchema;
import com.alibaba.tesla.appmanager.server.addon.BaseAddon;
import com.alibaba.tesla.appmanager.server.addon.req.ApplyAddonInstanceReq;
import com.alibaba.tesla.appmanager.server.addon.res.ApplyAddonRes;
import com.alibaba.tesla.appmanager.server.event.loader.AddonLoadedEvent;
import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统环境变量 - 资源 Addon
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Component("SystemEnvResourceAddon")
public class SystemEnvResourceAddon extends BaseAddon {

    @Getter
    private final ComponentTypeEnum addonType = ComponentTypeEnum.RESOURCE_ADDON;

    @Getter
    private final String addonId = "system-env";

    @Getter
    private final String addonVersion = "1.0";

    @Getter
    private final String addonLabel = "System Environment";

    @Getter
    private final String addonDescription = "System Environment";

    @Getter
    private final ComponentSchema addonSchema = SchemaUtil.toSchema(ComponentSchema.class, "\n" +
            "apiVersion: core.oam.dev/v1alpha2\n" +
            "kind: Component\n" +
            "metadata:\n" +
            "  name: RESOURCE_ADDON.system-env\n" +
            "spec:\n" +
            "  workload:\n" +
            "    apiVersion: apps.abm.io/v1\n" +
            "    kind: ResourceAddon\n" +
            "    metadata:\n" +
            "      name: system-env\n" +
            "    spec:\n" +
            "      keys: []");

    @Getter
    private final String addonConfigSchema = "";

    @Autowired
    private ApplicationEventPublisher publisher;

    /**
     * 初始化，注册自身
     */
    @PostConstruct
    public void init() {
        publisher.publishEvent(new AddonLoadedEvent(
                this, AddonUtil.combineAddonKey(getAddonType(), getAddonId()), this.getClass().getSimpleName()));
    }

    @Override
    public ApplyAddonRes apply(ApplyAddonInstanceReq request) {
        ComponentSchema schema = request.getSchema();
        JSONObject spec = (JSONObject) schema.getSpec().getWorkload().getSpec();
        JSONArray keys = spec.getJSONArray("keys");
        Map<String, Object> envVars = new HashMap<>();
        for (Object key : keys) {
            String keyStr = (String) key;
            String value = System.getenv(keyStr);
            if (StringUtils.isEmpty(value)) {
                value = "";
            }
            envVars.put(keyStr, value);
        }
        schema.overwriteWorkloadSpecVars(ImmutableMap.of("env", envVars));
        return ApplyAddonRes.builder()
                .componentSchema(schema)
                .signature(null)
                .build();
    }
}
