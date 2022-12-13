package cn.datax.service.data.metadata.mapstruct;

import cn.datax.common.mapstruct.EntityMapper;
import cn.datax.service.data.metadata.api.dto.MetadataColumnDto;
import cn.datax.service.data.metadata.api.entity.MetadataColumnEntity;
import cn.datax.service.data.metadata.api.vo.MetadataColumnVo;
import org.mapstruct.Mapper;

/**
 * <p>
 * 元数据信息表 Mapper 实体映射
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-29
 */
@Mapper(componentModel = "spring")
public interface MetadataColumnMapper extends EntityMapper<MetadataColumnDto, MetadataColumnEntity, MetadataColumnVo> {

}
