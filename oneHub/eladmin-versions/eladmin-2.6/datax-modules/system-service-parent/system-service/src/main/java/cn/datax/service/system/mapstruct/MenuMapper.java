package cn.datax.service.system.mapstruct;

import cn.datax.common.mapstruct.EntityMapper;
import cn.datax.service.system.api.dto.MenuDto;
import cn.datax.service.system.api.entity.MenuEntity;
import cn.datax.service.system.api.vo.MenuVo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MenuMapper extends EntityMapper<MenuDto, MenuEntity, MenuVo> {
}
