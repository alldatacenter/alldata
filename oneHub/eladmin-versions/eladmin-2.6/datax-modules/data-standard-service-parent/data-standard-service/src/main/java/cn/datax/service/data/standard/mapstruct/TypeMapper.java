package cn.datax.service.data.standard.mapstruct;

import cn.datax.common.mapstruct.EntityMapper;
import cn.datax.service.data.standard.api.dto.TypeDto;
import cn.datax.service.data.standard.api.entity.TypeEntity;
import cn.datax.service.data.standard.api.vo.TypeVo;
import org.mapstruct.Mapper;

/**
 * <p>
 * 数据标准类别表 Mapper 实体映射
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-26
 */
@Mapper(componentModel = "spring")
public interface TypeMapper extends EntityMapper<TypeDto, TypeEntity, TypeVo> {

}
