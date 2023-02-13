package com.platform.service.mapstruct;

import com.platform.base.BaseMapper;
import com.platform.domain.Log;
import com.platform.service.dto.LogErrorDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * @author AllDataDC
 * @date 2023-01-27
 */
@Mapper(componentModel = "spring",unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LogErrorMapper extends BaseMapper<LogErrorDTO, Log> {

}