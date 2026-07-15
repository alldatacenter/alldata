package cn.datax.service.system.mapstruct;

import cn.datax.common.mapstruct.EntityMapper;
import cn.datax.service.system.api.dto.DeptDto;
import cn.datax.service.system.api.entity.DeptEntity;
import cn.datax.service.system.api.vo.DeptVo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DeptMapper extends EntityMapper<DeptDto, DeptEntity, DeptVo> {
}
