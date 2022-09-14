package com.alibaba.tesla.appmanager.trait;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.definition.repository.condition.DefinitionSchemaQueryCondition;
import com.alibaba.tesla.appmanager.definition.repository.domain.DefinitionSchemaDO;
import com.alibaba.tesla.appmanager.definition.service.DefinitionSchemaService;
import com.alibaba.tesla.appmanager.domain.core.WorkloadResource;
import com.alibaba.tesla.appmanager.domain.schema.TraitDefinition;
import com.alibaba.tesla.appmanager.trait.repository.condition.TraitQueryCondition;
import com.alibaba.tesla.appmanager.trait.repository.domain.TraitDO;
import com.alibaba.tesla.appmanager.trait.service.TraitService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONTokener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Constructor;

/**
 * Trait 工厂
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class TraitFactory {

    private final TraitService traitService;

    private final DefinitionSchemaService definitionSchemaService;

    public TraitFactory(TraitService traitService, DefinitionSchemaService definitionSchemaService) {
        this.traitService = traitService;
        this.definitionSchemaService = definitionSchemaService;
    }

    /**
     * 获取 Trait 对象
     *
     * @param name Trait Name
     * @return Trait 对象，如果不存在则抛出异常
     */
    @Deprecated
    public Trait newInstance(String name, JSONObject spec, WorkloadResource workloadResource) {
        // Get trait from database
        TraitDO traitDO = traitService.get(TraitQueryCondition.builder().name(name).withBlobs(true).build(),
                DefaultConstant.SYSTEM_OPERATOR);
        if (traitDO == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, String.format("cannot find trait by name %s", name));
        }
        String className = traitDO.getClassName();

        // new trait instance
        try {
            TraitDefinition traitDefinition = newTraitDefinition(name);
            Class<?> cls = Class.forName(className);
            Constructor<?> constructor = cls.getConstructor(
                    String.class, TraitDefinition.class, JSONObject.class, WorkloadResource.class);
            return (Trait) constructor.newInstance(name, traitDefinition, spec, workloadResource);
        } catch (Exception e) {
            log.error("exception when init trait instance|name={}|spec={}|workloadResource={}|exception={}",
                    name, spec.toJSONString(), JSONObject.toJSONString(workloadResource), ExceptionUtils.getStackTrace(e));
            throw new AppException(AppErrorCode.UNKNOWN_ERROR, "exception when init trait instance", e);
        }
    }

    /**
     * 获取指定 trait 的 definition 对象
     *
     * @param name Trait 名称
     * @return TraitDefinition 对象
     */
    public TraitDefinition newTraitDefinition(String name) {
        TraitDO traitDO = traitService.get(TraitQueryCondition.builder().name(name).withBlobs(true).build(),
                DefaultConstant.SYSTEM_OPERATOR);
        if (traitDO == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, String.format("cannot find trait by name %s", name));
        }
        String definitionRef = traitDO.getDefinitionRef();
        String traitDefinitionStr = traitDO.getTraitDefinition();
        if (StringUtils.isEmpty(traitDefinitionStr)) {
            throw new AppException(AppErrorCode.UNKNOWN_ERROR,
                    String.format("empty trait definition found by name %s, traitDO=%s",
                            name, JSONObject.toJSONString(traitDO)));
        }

        // Get definition schema from database and validate
        DefinitionSchemaDO definitionSchemaDO = definitionSchemaService.get(
                DefinitionSchemaQueryCondition.builder().name(definitionRef).build(),
                DefaultConstant.SYSTEM_OPERATOR);
        if (definitionSchemaDO == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("cannot find definition schema by name %s", definitionRef));
        }
        String jsonSchema = definitionSchemaDO.getJsonSchema();

//        validate(jsonSchema, spec);
        return SchemaUtil.toSchema(TraitDefinition.class, traitDefinitionStr);
    }

    /**
     * 根据指定的 JsonSchema 校验 spec 是否符合要求
     *
     * @param jsonSchema Json Schema
     * @param spec       需要校验的对象
     */
    private void validate(String jsonSchema, JSONObject spec) {
        Schema schema = SchemaLoader.load(new org.json.JSONObject(new JSONTokener(jsonSchema)));
        schema.validate(new org.json.JSONObject(spec.toJSONString()));
    }
}
