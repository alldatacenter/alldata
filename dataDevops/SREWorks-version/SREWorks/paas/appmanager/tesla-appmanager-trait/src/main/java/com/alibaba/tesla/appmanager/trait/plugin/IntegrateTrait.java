package com.alibaba.tesla.appmanager.trait.plugin;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.domain.core.WorkloadResource;
import com.alibaba.tesla.appmanager.domain.req.kubectl.KubectlApplyReq;
import com.alibaba.tesla.appmanager.domain.schema.TraitDefinition;
import com.alibaba.tesla.appmanager.kubernetes.sevice.kubectl.KubectlService;
import com.alibaba.tesla.appmanager.spring.util.SpringBeanUtil;
import com.alibaba.tesla.appmanager.trait.BaseTrait;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

@Slf4j
public class IntegrateTrait extends BaseTrait {

    public IntegrateTrait(String name, TraitDefinition traitDefinition, JSONObject spec, WorkloadResource ref) {
        super(name, traitDefinition, spec, ref);
    }

    @Override
    public void execute() {
        JSONObject spec = getSpec();
        JSONObject content = spec.getJSONObject("content");
        JSONArray others = spec.getJSONArray("others");
        JSONObject workloadSpec = (JSONObject)getWorkloadRef().getSpec();
        mergeJsonObject(workloadSpec, content);
        applyOthers(others);
        log.info("IntegrateTrait workloadSpec: \n" + JSONObject.toJSONString(workloadSpec, true) +
            "\n others: \n" + JSONObject.toJSONString(others, true));
    }

    private void mergeJsonArray(JSONArray j1, JSONArray j2) {
        try {
            for (int i = 0; i < j1.size(); i++) {
                Object v1 = j1.get(i);
                Object v2 = j2.get(i);
                mergeJsonObject((JSONObject)v1, (JSONObject)v2);
            }
            for (int i = 0; i < j2.size(); i++) {
                if (i >= j1.size()) {
                    j1.add(j2.get(i));
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void mergeJsonObject(JSONObject j1, JSONObject j2) {
        if (CollectionUtils.isEmpty(j2)) {
            return;
        }
        for (String key : j1.keySet()) {
            if (j2.containsKey(key)) {
                Object v1 = JSON.toJSON(j1.get(key));
                Object v2 = JSON.toJSON(j2.get(key));
                if (v1 instanceof JSONObject && v2 instanceof JSONObject) {
                    mergeJsonObject((JSONObject)v1, (JSONObject)v2);
                } else if (v1 instanceof JSONArray && v2 instanceof JSONArray) {
                    mergeJsonArray((JSONArray)v1, (JSONArray)v2);
                } else {
                    j1.put(key, v2);
                }
            }
        }
        for (String key : j2.keySet()) {
            if (!j1.containsKey(key)) {
                j1.put(key, j2.get(key));
            }
        }
    }

    private void applyOthers(JSONArray others) {
        if (CollectionUtils.isEmpty(others)) {
            return;
        }
        KubectlService kubectlService = SpringBeanUtil.getBean(KubectlService.class);
        for (JSONObject other : others.toJavaList(JSONObject.class)) {
            kubectlService.apply(
                KubectlApplyReq.builder()
                    .clusterId("master")
                    .namespaceId("default")
                    .content(other)
                    .build(),
                DefaultConstant.SYSTEM_OPERATOR);
        }
    }
}
