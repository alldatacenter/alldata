package com.alibaba.tesla.appmanager.server.assembly;

import com.alibaba.tesla.appmanager.common.assembly.BaseDtoConvert;
import com.alibaba.tesla.appmanager.domain.dto.ComponentPackageDTO;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageDO;
import org.springframework.stereotype.Component;

/**
 * 组件包 DTO 转换器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Component
public class ComponentPackageDtoConvert extends BaseDtoConvert<ComponentPackageDTO, ComponentPackageDO> {

    public ComponentPackageDtoConvert() {
        super(ComponentPackageDTO.class, ComponentPackageDO.class);
    }
}
