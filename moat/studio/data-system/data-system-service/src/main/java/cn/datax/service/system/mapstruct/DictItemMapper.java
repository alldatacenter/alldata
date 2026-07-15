package cn.datax.service.system.mapstruct;

import cn.datax.common.mapstruct.EntityMapper;
import cn.datax.service.system.api.dto.DictItemDto;
import cn.datax.service.system.api.entity.DictItemEntity;
import cn.datax.service.system.api.vo.DictItemVo;
import org.mapstruct.Mapper;

/**
 * <p>
 * 字典项信息表 Mapper 实体映射
 * </p>
 *
 * @author yuwei
 * @date 2022-04-17
 */
@Mapper(componentModel = "spring")
public interface DictItemMapper extends EntityMapper<DictItemDto, DictItemEntity, DictItemVo> {

}
