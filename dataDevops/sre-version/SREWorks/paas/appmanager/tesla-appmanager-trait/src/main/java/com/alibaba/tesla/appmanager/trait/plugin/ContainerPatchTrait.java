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
 * container 层面的 patch trait
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class ContainerPatchTrait extends BaseTrait {

    /**
     * 初始化
     *  @param name            名称
     * @param traitDefinition Trait 定义
     * @param spec            当前 Trait Spec (options)
     * @param ref             当前 Trait 绑定的 workload ref 引用
     */
    public ContainerPatchTrait(String name, TraitDefinition traitDefinition, JSONObject spec, WorkloadResource ref) {
        super(name, traitDefinition, spec, ref);
    }

    @Override
    public void execute() {
        JSONObject spec = getSpec();
        JSONObject patches = getSpec().getJSONObject("patches");
        JSONArray targets = getSpec().getJSONArray("targets");
        for (int i = 0; i < targets.size(); i++) {
            // 参数检测
            JSONObject target = targets.getJSONObject(i);
            String targetContainer = target.getString("container");
            String key = target.getString("key");
            if (StringUtils.isAnyEmpty(targetContainer, key)) {
                String errorMessage = String.format("container or key not found %s", target.toJSONString());
                throw new AppException(AppErrorCode.INVALID_USER_ARGS, errorMessage);
            }

            // 获取 patch 对象
            JSONObject patchObject = patches.getJSONObject(key);
            if (patchObject == null) {
                throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                        String.format("cannot find patch object %s in patches", key));
            }

            // 操作 patch
            patchContainer(targetContainer, patchObject);
        }

        log.info("container patch trait has applied to workload {}|spec={}",
                JSONObject.toJSONString(getWorkloadRef().getMetadata()), spec.toJSONString());
    }

    /**
     * 注入 patchObject 到指定 name 名称对应的 container 中
     * @param name container name
     * @param patchObject 要 patch 的对象
     */
    private void patchContainer(String name, JSONObject patchObject) {
        JSONObject workloadSpec = (JSONObject) getWorkloadRef().getSpec();
        JSONArray containers;
        if (workloadSpec.get("cloneSet") != null) {
            containers = workloadSpec
                    .getJSONObject("cloneSet")
                    .getJSONObject("template")
                    .getJSONObject("spec")
                    .getJSONArray("containers");
        } else if (workloadSpec.get("advancedStatefulSet") != null) {
            containers = workloadSpec
                    .getJSONObject("advancedStatefulSet")
                    .getJSONObject("template")
                    .getJSONObject("spec")
                    .getJSONArray("containers");
        } else {
            containers = workloadSpec.getJSONArray("containers");
        }

        // 寻找并注入
        for (Object containerObj : containers) {
            JSONObject container = (JSONObject) containerObj;
            if (name.equals(container.getString("name"))) {
                container.putAll(patchObject);
            }
        }
    }
}
