package com.alibaba.tesla.appmanager.trait.plugin;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.domain.core.WorkloadResource;
import com.alibaba.tesla.appmanager.domain.schema.TraitDefinition;
import com.alibaba.tesla.appmanager.trait.BaseTrait;
import lombok.extern.slf4j.Slf4j;

/**
 * 用于单元测试，测试 trait 修改 workload 内部属性
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class TestModifyWorkloadTrait extends BaseTrait {

    public TestModifyWorkloadTrait(String name, TraitDefinition traitDefinition, JSONObject spec, WorkloadResource ref) {
        super(name, traitDefinition, spec, ref);
    }

    @Override
    public void execute() {
        JSONObject spec = getSpec();
        JSONObject workloadSpec = (JSONObject) getWorkloadRef().getSpec();
        String key = spec.getString("key");
        String value = spec.getString("value");
        workloadSpec.put(key, value);
        log.info("test modify workload finished||key={}||value={}||workloadRef={}",
            key, value, JSONObject.toJSONString(getWorkloadRef()));
    }
}
