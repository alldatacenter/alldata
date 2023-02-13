package cn.datax.service.data.masterdata.mapstruct;

import cn.datax.common.mapstruct.EntityMapper;
import cn.datax.service.data.masterdata.api.dto.ModelDto;
import cn.datax.service.data.masterdata.api.entity.ModelEntity;
import cn.datax.service.data.masterdata.api.vo.ModelVo;
import org.mapstruct.Mapper;

/**
 * <p>
 * 主数据模型表 Mapper 实体映射
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-26
 */
@Mapper(componentModel = "spring")
public interface ModelMapstruct extends EntityMapper<ModelDto, ModelEntity, ModelVo> {

}
