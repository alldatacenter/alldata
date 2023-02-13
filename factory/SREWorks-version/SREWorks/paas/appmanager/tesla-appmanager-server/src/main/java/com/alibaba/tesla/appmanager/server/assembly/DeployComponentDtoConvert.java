package com.alibaba.tesla.appmanager.server.assembly;

import com.alibaba.tesla.appmanager.common.assembly.BaseDtoConvert;
import com.alibaba.tesla.appmanager.common.util.ClassUtil;
import com.alibaba.tesla.appmanager.domain.dto.DeployComponentDTO;
import com.alibaba.tesla.appmanager.server.repository.domain.DeployComponentDO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.stereotype.Component;

/**
 * Deploy Component DTO 转换器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Component
public class DeployComponentDtoConvert extends BaseDtoConvert<DeployComponentDTO, DeployComponentDO> {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public DeployComponentDtoConvert() {
        super(DeployComponentDTO.class, DeployComponentDO.class);
    }

    @Override
    public DeployComponentDTO to(DeployComponentDO taskDO) {
        if (taskDO == null) {
            return null;
        }
        DeployComponentDTO result = new DeployComponentDTO();
        ClassUtil.copy(taskDO, result);
        if (taskDO.getGmtCreate() != null) {
            result.setReadableGmtCreate(DateFormatUtils.format(taskDO.getGmtCreate(), DATE_FORMAT));
        }
        if (taskDO.getGmtModified() != null) {
            result.setReadableGmtModified(DateFormatUtils.format(taskDO.getGmtModified(), DATE_FORMAT));
        }
        if (taskDO.getGmtStart() != null) {
            result.setReadableGmtStart(DateFormatUtils.format(taskDO.getGmtStart(), DATE_FORMAT));
        }
        if (taskDO.getGmtEnd() != null) {
            result.setReadableGmtEnd(DateFormatUtils.format(taskDO.getGmtEnd(), DATE_FORMAT));
        }
        result.setCost(taskDO.costTime());
        return result;
    }
}
