package com.alibaba.tesla.appmanager.dynamicscript.assembly;

import com.alibaba.tesla.appmanager.common.assembly.BaseDtoConvert;
import com.alibaba.tesla.appmanager.domain.dto.DynamicScriptDTO;
import com.alibaba.tesla.appmanager.dynamicscript.repository.domain.DynamicScriptDO;
import org.springframework.stereotype.Component;

/**
 * 动态脚本 DTO 转换器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Component
public class DynamicScriptConvert extends BaseDtoConvert<DynamicScriptDTO, DynamicScriptDO> {

    public DynamicScriptConvert() {
        super(DynamicScriptDTO.class, DynamicScriptDO.class);
    }
}
