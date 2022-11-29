package cn.datax.service.system.mapstruct;

import cn.datax.common.mapstruct.EntityMapper;
import cn.datax.service.system.api.dto.RoleDto;
import cn.datax.service.system.api.entity.RoleEntity;
import cn.datax.service.system.api.vo.RoleVo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleMapper extends EntityMapper<RoleDto, RoleEntity, RoleVo> {
}
