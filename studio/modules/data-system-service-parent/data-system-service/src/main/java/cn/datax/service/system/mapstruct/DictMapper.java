package cn.datax.service.system.mapstruct;

import cn.datax.common.mapstruct.EntityMapper;
import cn.datax.service.system.api.dto.DictDto;
import cn.datax.service.system.api.entity.DictEntity;
import cn.datax.service.system.api.vo.DictVo;
import org.mapstruct.Mapper;

/**
 * <p>
 * 字典编码信息表 Mapper 实体映射
 * </p>
 *
 * @author yuwei
 * @date 2022-04-17
 */
@Mapper(componentModel = "spring")
public interface DictMapper extends EntityMapper<DictDto, DictEntity, DictVo> {

}
