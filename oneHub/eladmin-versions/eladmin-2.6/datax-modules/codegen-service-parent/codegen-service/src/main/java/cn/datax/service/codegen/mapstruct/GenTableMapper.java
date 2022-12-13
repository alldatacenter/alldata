package cn.datax.service.codegen.mapstruct;

import cn.datax.common.mapstruct.EntityMapper;
import cn.datax.service.codegen.api.dto.GenTableDto;
import cn.datax.service.codegen.api.entity.GenTableEntity;
import cn.datax.service.codegen.api.vo.GenTableVo;
import org.mapstruct.Mapper;

/**
 * <p>
 * 代码生成信息表 Mapper 实体映射
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-19
 */
@Mapper(componentModel = "spring")
public interface GenTableMapper extends EntityMapper<GenTableDto, GenTableEntity, GenTableVo> {

}
