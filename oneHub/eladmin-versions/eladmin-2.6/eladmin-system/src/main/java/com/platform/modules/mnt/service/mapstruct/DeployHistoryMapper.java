
package com.platform.modules.mnt.service.mapstruct;

import com.platform.base.BaseMapper;
import com.platform.modules.mnt.domain.DeployHistory;
import com.platform.modules.mnt.service.dto.DeployHistoryDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
* @author zhanghouying
* @date 2022-10-27
*/
@Mapper(componentModel = "spring",uses = {},unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DeployHistoryMapper extends BaseMapper<DeployHistoryDto, DeployHistory> {

}
