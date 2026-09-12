
package com.platform.modules.mnt.service.mapstruct;

import com.platform.base.BaseMapper;
import com.platform.modules.mnt.domain.Deploy;
import com.platform.modules.mnt.service.dto.DeployDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
* @author AllDataDC
* @date 2023-01-27
*/
@Mapper(componentModel = "spring",uses = {AppMapper.class, ServerDeployMapper.class},unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DeployMapper extends BaseMapper<DeployDto, Deploy> {

}
