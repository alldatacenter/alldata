
package com.platform.modules.system.service.mapstruct;

import com.platform.base.BaseMapper;
import com.platform.modules.system.domain.Job;
import com.platform.modules.system.service.dto.JobSmallDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
* @author AllDataDC
* @date 2023-01-27 
*/
@Mapper(componentModel = "spring",unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface JobSmallMapper extends BaseMapper<JobSmallDto, Job> {

}