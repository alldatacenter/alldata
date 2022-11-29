package cn.datax.service.data.market.mapping.mapstruct;

import cn.datax.common.mapstruct.EntityMapper;
import cn.datax.service.data.market.api.dto.ApiLogDto;
import cn.datax.service.data.market.api.entity.ApiLogEntity;
import cn.datax.service.data.market.api.vo.ApiLogVo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ApiLogMapper extends EntityMapper<ApiLogDto, ApiLogEntity, ApiLogVo> {
}
