package cn.datax.service.system.mapstruct;

import cn.datax.common.mapstruct.EntityMapper;
import cn.datax.service.system.api.dto.LogDto;
import cn.datax.service.system.api.entity.LogEntity;
import cn.datax.service.system.api.vo.LogVo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LogMapper extends EntityMapper<LogDto, LogEntity, LogVo> {
}
