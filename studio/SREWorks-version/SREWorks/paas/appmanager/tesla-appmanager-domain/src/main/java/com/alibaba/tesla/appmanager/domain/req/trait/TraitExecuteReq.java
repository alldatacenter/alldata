package com.alibaba.tesla.appmanager.domain.req.trait;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.domain.core.WorkloadResource;
import com.alibaba.tesla.appmanager.domain.schema.DeployAppSchema;
import com.alibaba.tesla.appmanager.domain.schema.TraitDefinition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Trait 执行侧请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraitExecuteReq {

    /**
     * 当前 trait 的唯一标识名称
     */
    private String name;

    /**
     * 当前 trait 的 definition 对象，包含基本元信息
     */
    private TraitDefinition traitDefinition;

    /**
     * 当前 trait 的配置 spec 信息
     */
    private JSONObject spec;

    /**
     * 绑定到当前 Trait 的 Workload Resource 引用
     */
    private WorkloadResource ref;

    /**
     * 绑定到当前 Trait 的 SpecComponent 对象
     */
    private DeployAppSchema.SpecComponent component;

    /**
     * Owner Reference
     */
    private String ownerReference;
}
