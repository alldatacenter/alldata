package cn.datax.service.data.standard.mapstruct;

import cn.datax.common.mapstruct.EntityMapper;
import cn.datax.service.data.standard.api.dto.DictDto;
import cn.datax.service.data.standard.api.entity.DictEntity;
import cn.datax.service.data.standard.api.vo.DictVo;
import org.mapstruct.Mapper;

/**
 * <p>
 * 数据标准字典表 Mapper 实体映射
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-26
 */
@Mapper(componentModel = "spring")
public interface DictMapper extends EntityMapper<DictDto, DictEntity, DictVo> {

}
