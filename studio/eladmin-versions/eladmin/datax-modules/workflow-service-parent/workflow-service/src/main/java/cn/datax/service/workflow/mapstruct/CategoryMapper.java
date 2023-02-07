package cn.datax.service.workflow.mapstruct;

import cn.datax.common.mapstruct.EntityMapper;
import cn.datax.service.workflow.api.dto.CategoryDto;
import cn.datax.service.workflow.api.entity.CategoryEntity;
import cn.datax.service.workflow.api.vo.CategoryVo;
import org.mapstruct.Mapper;

/**
 * <p>
 * 流程分类表 Mapper 实体映射
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-10
 */
@Mapper(componentModel = "spring")
public interface CategoryMapper extends EntityMapper<CategoryDto, CategoryEntity, CategoryVo> {

}
