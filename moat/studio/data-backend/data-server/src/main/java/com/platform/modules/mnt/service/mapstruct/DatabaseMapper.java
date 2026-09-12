
package com.platform.modules.mnt.service.mapstruct;

import com.platform.base.BaseMapper;
import com.platform.modules.mnt.domain.Database;
import com.platform.modules.mnt.service.dto.DatabaseDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
* @author AllDataDC
* @date 2023-01-27
*/
@Mapper(componentModel = "spring",unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DatabaseMapper extends BaseMapper<DatabaseDto, Database> {

}
