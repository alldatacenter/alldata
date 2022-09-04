package com.alibaba.tesla.appmanager.trait.plugin;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.domain.core.WorkloadResource;
import com.alibaba.tesla.appmanager.domain.schema.TraitDefinition;
import com.alibaba.tesla.appmanager.trait.BaseTrait;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 字符串转对象 Trait
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class Str2ObjTrait extends BaseTrait {

    public Str2ObjTrait(String name, TraitDefinition traitDefinition, JSONObject spec, WorkloadResource ref) {
        super(name, traitDefinition, spec, ref);
    }

    @Override
    public void execute() {
        Yaml yaml = SchemaUtil.createYaml(Arrays.asList(Object.class, JSONObject.class));
        JSONObject spec = getSpec();
        Map<String, Object> mid = new HashMap<>();
        for (Map.Entry<String, Object> entry : spec.entrySet()) {
            if (!(entry.getValue() instanceof String)) {
                throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("invalid key %s in str2obj trait, not string", entry.getKey()));
            }
            mid.put(entry.getKey(), yaml.load((String) entry.getValue()));
        }
        spec.putAll(mid);
    }
}
