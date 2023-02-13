package com.alibaba.tesla.appmanager.server.assembly;

import com.alibaba.tesla.appmanager.common.assembly.BaseDtoConvert;
import com.alibaba.tesla.appmanager.common.util.ClassUtil;
import com.alibaba.tesla.appmanager.common.util.VersionUtil;
import com.alibaba.tesla.appmanager.domain.dto.AppPackageTaskDTO;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageTaskDO;
import org.springframework.stereotype.Component;

/**
 * 应用包 DTO 转换器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Component
public class AppPackageTaskDtoConvert extends BaseDtoConvert<AppPackageTaskDTO, AppPackageTaskDO> {

    public AppPackageTaskDtoConvert() {
        super(AppPackageTaskDTO.class, AppPackageTaskDO.class);
    }

    @Override
    public AppPackageTaskDTO to(AppPackageTaskDO taskDO) {
        if (taskDO == null) {
            return null;
        }
        AppPackageTaskDTO result = new AppPackageTaskDTO();
        ClassUtil.copy(taskDO, result);
        result.setSimplePackageVersion(VersionUtil.clear(taskDO.getPackageVersion()));
        return result;
    }
}
