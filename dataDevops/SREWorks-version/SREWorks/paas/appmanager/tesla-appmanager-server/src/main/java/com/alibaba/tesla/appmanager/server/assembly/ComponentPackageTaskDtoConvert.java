package com.alibaba.tesla.appmanager.server.assembly;

import com.alibaba.tesla.appmanager.common.assembly.BaseDtoConvert;
import com.alibaba.tesla.appmanager.common.util.ClassUtil;
import com.alibaba.tesla.appmanager.common.util.VersionUtil;
import com.alibaba.tesla.appmanager.domain.dto.ComponentPackageTaskDTO;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageTaskDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Component Package Task DTO 转换器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Component
public class ComponentPackageTaskDtoConvert extends BaseDtoConvert<ComponentPackageTaskDTO, ComponentPackageTaskDO> {

    public ComponentPackageTaskDtoConvert() {
        super(ComponentPackageTaskDTO.class, ComponentPackageTaskDO.class);
    }

    @Override
    public ComponentPackageTaskDTO to(ComponentPackageTaskDO taskDO) {
        if (taskDO == null) {
            return null;
        }
        ComponentPackageTaskDTO result = new ComponentPackageTaskDTO();
        ClassUtil.copy(taskDO, result);
        result.setSimplePackageVersion(VersionUtil.clear(taskDO.getPackageVersion()));
        return result;
    }
}
