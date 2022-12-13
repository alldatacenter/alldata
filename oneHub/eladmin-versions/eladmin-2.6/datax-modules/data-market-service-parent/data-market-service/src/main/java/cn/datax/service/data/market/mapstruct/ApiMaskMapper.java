package cn.datax.service.data.market.mapstruct;

import cn.datax.common.mapstruct.EntityMapper;
import cn.datax.service.data.market.api.dto.ApiMaskDto;
import cn.datax.service.data.market.api.entity.ApiMaskEntity;
import cn.datax.service.data.market.api.vo.ApiMaskVo;
import org.mapstruct.Mapper;

/**
 * <p>
 * 数据API脱敏信息表 Mapper 实体映射
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
@Mapper(componentModel = "spring")
public interface ApiMaskMapper extends EntityMapper<ApiMaskDto, ApiMaskEntity, ApiMaskVo> {

}
