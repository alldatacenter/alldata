
package com.platform.modules.system.service.mapstruct;

import com.platform.base.BaseMapper;
import com.platform.modules.system.domain.Menu;
import com.platform.modules.system.service.dto.MenuDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * @author AllDataDC
 * @date 2023-01-27
 */
@Mapper(componentModel = "spring",unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MenuMapper extends BaseMapper<MenuDto, Menu> {
}
