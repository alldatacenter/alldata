package cn.datax.service.data.visual.mapstruct;

import cn.datax.common.mapstruct.EntityMapper;
import cn.datax.service.data.visual.api.dto.BoardDto;
import cn.datax.service.data.visual.api.entity.BoardEntity;
import cn.datax.service.data.visual.api.vo.BoardVo;
import org.mapstruct.Mapper;

/**
 * <p>
 * 可视化看板配置信息表 Mapper 实体映射
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-04
 */
@Mapper(componentModel = "spring")
public interface BoardMapper extends EntityMapper<BoardDto, BoardEntity, BoardVo> {

}
