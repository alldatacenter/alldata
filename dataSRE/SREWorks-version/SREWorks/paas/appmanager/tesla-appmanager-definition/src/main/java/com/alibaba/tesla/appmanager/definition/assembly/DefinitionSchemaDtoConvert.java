package com.alibaba.tesla.appmanager.definition.assembly;

import com.alibaba.tesla.appmanager.common.assembly.BaseDtoConvert;
import com.alibaba.tesla.appmanager.domain.dto.DefinitionSchemaDTO;
import com.alibaba.tesla.appmanager.definition.repository.domain.DefinitionSchemaDO;
import org.springframework.stereotype.Component;

/**
 * Definition Schema DTO 转换器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Component
public class DefinitionSchemaDtoConvert extends BaseDtoConvert<DefinitionSchemaDTO, DefinitionSchemaDO> {

    public DefinitionSchemaDtoConvert() {
        super(DefinitionSchemaDTO.class, DefinitionSchemaDO.class);
    }
}
