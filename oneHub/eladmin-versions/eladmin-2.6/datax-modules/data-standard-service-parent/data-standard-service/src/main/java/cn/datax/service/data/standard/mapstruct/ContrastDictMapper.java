package cn.datax.service.data.standard.mapstruct;

import cn.datax.common.mapstruct.EntityMapper;
import cn.datax.service.data.standard.api.dto.ContrastDictDto;
import cn.datax.service.data.standard.api.entity.ContrastDictEntity;
import cn.datax.service.data.standard.api.vo.ContrastDictVo;
import org.mapstruct.Mapper;

/**
 * <p>
 * 字典对照信息表 Mapper 实体映射
 * </p>
 *
 * @author yuwei
 * @since 2020-09-27
 */
@Mapper(componentModel = "spring")
public interface ContrastDictMapper extends EntityMapper<ContrastDictDto, ContrastDictEntity, ContrastDictVo> {

}
