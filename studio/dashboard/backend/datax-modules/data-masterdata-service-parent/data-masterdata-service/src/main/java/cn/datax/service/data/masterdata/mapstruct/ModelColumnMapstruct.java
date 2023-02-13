package cn.datax.service.data.masterdata.mapstruct;

import cn.datax.common.mapstruct.EntityMapper;
import cn.datax.service.data.masterdata.api.dto.ModelColumnDto;
import cn.datax.service.data.masterdata.api.entity.ModelColumnEntity;
import cn.datax.service.data.masterdata.api.vo.ModelColumnVo;
import org.mapstruct.Mapper;

/**
 * <p>
 * 主数据模型列信息表 Mapper 实体映射
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-26
 */
@Mapper(componentModel = "spring")
public interface ModelColumnMapstruct extends EntityMapper<ModelColumnDto, ModelColumnEntity, ModelColumnVo> {

}
