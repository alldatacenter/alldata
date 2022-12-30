package com.alibaba.tesla.appmanager.trait.plugin;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.domain.core.WorkloadResource;
import com.alibaba.tesla.appmanager.domain.schema.TraitDefinition;
import com.alibaba.tesla.appmanager.trait.BaseTrait;
import lombok.extern.slf4j.Slf4j;

/**
 * pod 层面的 patch trait
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class PodPatchTrait extends BaseTrait {

    /**
     * 初始化
     *
     * @param name            名称
     * @param traitDefinition Trait 定义
     * @param spec            当前 Trait Spec (options)
     * @param ref             当前 Trait 绑定的 workload ref 引用
     */
    public PodPatchTrait(String name, TraitDefinition traitDefinition, JSONObject spec, WorkloadResource ref) {
        super(name, traitDefinition, spec, ref);
    }

    @Override
    public void execute() {
        JSONObject spec = getSpec();
        JSONObject metadata = getSpec().getJSONObject("metadata");
        if (metadata == null) {
            return;
        }

        WorkloadResource workloadRef = getWorkloadRef();
        JSONObject labels = metadata.getJSONObject("labels");
        JSONObject annotations = metadata.getJSONObject("annotations");
        if (labels != null) {
            ((JSONObject) workloadRef.getMetadata().getLabels()).putAll(labels);
        }
        if (annotations != null) {
            ((JSONObject) workloadRef.getMetadata().getAnnotations()).putAll(annotations);
        }
        log.info("pod patch trait has applied to workload {}|spec={}",
                JSONObject.toJSONString(getWorkloadRef().getMetadata()), spec.toJSONString());
    }
}
