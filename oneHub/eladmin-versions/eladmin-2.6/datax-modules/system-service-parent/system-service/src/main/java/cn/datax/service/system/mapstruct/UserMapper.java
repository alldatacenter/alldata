package cn.datax.service.system.mapstruct;

import cn.datax.common.mapstruct.EntityMapper;
import cn.datax.service.system.api.dto.UserDto;
import cn.datax.service.system.api.entity.UserEntity;
import cn.datax.service.system.api.vo.UserVo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper extends EntityMapper<UserDto, UserEntity, UserVo> {
}
