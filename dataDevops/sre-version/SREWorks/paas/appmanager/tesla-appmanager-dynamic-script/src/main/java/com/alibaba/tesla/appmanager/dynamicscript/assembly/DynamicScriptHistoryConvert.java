package com.alibaba.tesla.appmanager.dynamicscript.assembly;

import com.alibaba.tesla.appmanager.common.assembly.BaseDtoConvert;
import com.alibaba.tesla.appmanager.domain.dto.DynamicScriptHistoryDTO;
import com.alibaba.tesla.appmanager.dynamicscript.repository.domain.DynamicScriptHistoryDO;
import org.springframework.stereotype.Component;

/**
 * 动态脚本_历史 DTO 转换器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Component
public class DynamicScriptHistoryConvert extends BaseDtoConvert<DynamicScriptHistoryDTO, DynamicScriptHistoryDO> {

    public DynamicScriptHistoryConvert() {
        super(DynamicScriptHistoryDTO.class, DynamicScriptHistoryDO.class);
    }
}
