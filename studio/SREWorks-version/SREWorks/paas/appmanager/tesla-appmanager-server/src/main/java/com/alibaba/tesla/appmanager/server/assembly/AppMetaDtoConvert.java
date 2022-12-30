package com.alibaba.tesla.appmanager.server.assembly;

import com.alibaba.tesla.appmanager.common.assembly.BaseDtoConvert;
import com.alibaba.tesla.appmanager.domain.dto.AppMetaDTO;
import com.alibaba.tesla.appmanager.server.repository.domain.AppMetaDO;
import org.springframework.stereotype.Component;

/**
 * 应用包 DTO 转换器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Component
public class AppMetaDtoConvert extends BaseDtoConvert<AppMetaDTO, AppMetaDO> {

    public AppMetaDtoConvert() {
        super(AppMetaDTO.class, AppMetaDO.class);
    }
}
