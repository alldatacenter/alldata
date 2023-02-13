package cn.datax.service.data.market.mapstruct;

import cn.datax.common.mapstruct.EntityMapper;
import cn.datax.service.data.market.api.dto.DataApiDto;
import cn.datax.service.data.market.api.entity.DataApiEntity;
import cn.datax.service.data.market.api.vo.DataApiVo;
import org.mapstruct.Mapper;

/**
 * <p>
 * 数据API信息表 Mapper 实体映射
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-31
 */
@Mapper(componentModel = "spring")
public interface DataApiMapper extends EntityMapper<DataApiDto, DataApiEntity, DataApiVo> {

}
