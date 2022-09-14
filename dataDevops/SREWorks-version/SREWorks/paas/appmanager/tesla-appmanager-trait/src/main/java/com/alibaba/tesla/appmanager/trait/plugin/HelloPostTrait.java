package com.alibaba.tesla.appmanager.trait.plugin;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.domain.core.WorkloadResource;
import com.alibaba.tesla.appmanager.domain.schema.TraitDefinition;
import com.alibaba.tesla.appmanager.trait.BaseTrait;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HelloPostTrait extends BaseTrait {

    public HelloPostTrait(String name, TraitDefinition traitDefinition, JSONObject spec, WorkloadResource ref) {
        super(name, traitDefinition, spec, ref);
    }

    @Override
    public void execute() {
        log.info("enter hello post trait|spec={}", getSpec().toJSONString());
        log.info("enter hello post trait|workloadRef={}", JSONObject.toJSONString(getWorkloadRef()));
    }
}
