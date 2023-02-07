package cn.datax.service.data.visual.mapstruct;

import cn.datax.common.mapstruct.EntityMapper;
import cn.datax.service.data.visual.api.dto.ChartDto;
import cn.datax.service.data.visual.api.entity.ChartEntity;
import cn.datax.service.data.visual.api.vo.ChartVo;
import org.mapstruct.Mapper;

/**
 * <p>
 * 可视化图表配置信息表 Mapper 实体映射
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-04
 */
@Mapper(componentModel = "spring")
public interface ChartMapper extends EntityMapper<ChartDto, ChartEntity, ChartVo> {

}
