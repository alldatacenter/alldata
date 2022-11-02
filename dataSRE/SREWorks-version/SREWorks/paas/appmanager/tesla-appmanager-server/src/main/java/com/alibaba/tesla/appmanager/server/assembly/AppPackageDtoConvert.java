package com.alibaba.tesla.appmanager.server.assembly;

import com.alibaba.tesla.appmanager.common.assembly.BaseDtoConvert;
import com.alibaba.tesla.appmanager.common.util.ClassUtil;
import com.alibaba.tesla.appmanager.common.util.VersionUtil;
import com.alibaba.tesla.appmanager.domain.dto.AppPackageDTO;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageDO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 应用包 DTO 转换器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Component
public class AppPackageDtoConvert extends BaseDtoConvert<AppPackageDTO, AppPackageDO> {

    public AppPackageDtoConvert() {
        super(AppPackageDTO.class, AppPackageDO.class);
    }

    @Override
    public AppPackageDTO to(AppPackageDO taskDO) {
        if (taskDO == null) {
            return null;
        }
        AppPackageDTO result = new AppPackageDTO();
        ClassUtil.copy(taskDO, result);
        if (StringUtils.isEmpty(result.getAppName())) {
            result.setAppName(taskDO.getAppId());
        }
        result.setSimplePackageVersion(VersionUtil.clear(taskDO.getPackageVersion()));
        return result;
    }
}
