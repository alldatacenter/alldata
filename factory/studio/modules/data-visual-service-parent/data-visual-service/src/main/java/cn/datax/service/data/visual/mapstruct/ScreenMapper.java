package cn.datax.service.data.visual.mapstruct;

import cn.datax.common.mapstruct.EntityMapper;
import cn.datax.service.data.visual.api.dto.ScreenDto;
import cn.datax.service.data.visual.api.entity.ScreenEntity;
import cn.datax.service.data.visual.api.vo.ScreenVo;
import org.mapstruct.Mapper;

/**
 * <p>
 * 可视化酷屏配置信息表 Mapper 实体映射
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-15
 */
@Mapper(componentModel = "spring")
public interface ScreenMapper extends EntityMapper<ScreenDto, ScreenEntity, ScreenVo> {

}
