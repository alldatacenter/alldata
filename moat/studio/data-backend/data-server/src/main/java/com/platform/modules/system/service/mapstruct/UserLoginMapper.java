
package com.platform.modules.system.service.mapstruct;

import com.platform.base.BaseMapper;
import com.platform.modules.system.domain.User;
import com.platform.modules.system.service.dto.UserLoginDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * @author AllDataDC
 * @date 2023-01-27
 */
@Mapper(componentModel = "spring",uses = {RoleMapper.class, DeptMapper.class, JobMapper.class},unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserLoginMapper extends BaseMapper<UserLoginDto, User> {
}
