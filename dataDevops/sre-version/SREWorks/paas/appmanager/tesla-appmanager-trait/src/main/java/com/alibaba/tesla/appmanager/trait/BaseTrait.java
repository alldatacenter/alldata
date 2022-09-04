package com.alibaba.tesla.appmanager.trait;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.domain.core.WorkloadResource;
import com.alibaba.tesla.appmanager.domain.schema.DeployAppSchema;
import com.alibaba.tesla.appmanager.domain.schema.TraitDefinition;
import org.apache.commons.lang3.NotImplementedException;

/**
 * Trait 实现基类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class BaseTrait implements Trait {

    /**
     * 当前 trait 的唯一标识名称
     */
    private final String name;

    /**
     * 当前 trait 的 definition 对象，包含基本元信息
     */
    private final TraitDefinition traitDefinition;

    /**
     * 当前 trait 的配置 spec 信息
     */
    private final JSONObject spec;

    /**
     * 绑定到当前 Trait 的 Workload Resource 引用
     */
    private final WorkloadResource ref;

    /**
     * Owner Reference
     */
    private String ownerReference;

    /**
     * 绑定到当前 Trait 的 SpecComponent 对象
     */
    private DeployAppSchema.SpecComponent component;

    /**
     * 初始化
     *
     * @param name            名称
     * @param traitDefinition Trait 定义
     * @param spec            当前 Trait Spec (options)
     * @param ref             当前 Trait 绑定的 workload ref 引用
     */
    public BaseTrait(String name, TraitDefinition traitDefinition, JSONObject spec, WorkloadResource ref) {
        this.name = name;
        this.traitDefinition = traitDefinition;
        this.spec = spec;
        this.ref = ref;
    }

    /**
     * 获取当前被绑定的 workload resource 的引用对象
     *
     * @return WorkloadResource
     */
    @Override
    public WorkloadResource getWorkloadRef() {
        return ref;
    }

    @Override
    public JSONObject getSpec() {
        return spec;
    }

    @Override
    public void setComponent(DeployAppSchema.SpecComponent component) {
        this.component = component;
    }

    @Override
    public String getOwnerReference() {
        return this.ownerReference;
    }

    @Override
    public void setOwnerReference(String ownerReference) {
        this.ownerReference = ownerReference;
    }

    @Override
    public DeployAppSchema.SpecComponent getComponent() {
        return component;
    }

    @Override
    public void execute() {
        throw new NotImplementedException();
    }
}
