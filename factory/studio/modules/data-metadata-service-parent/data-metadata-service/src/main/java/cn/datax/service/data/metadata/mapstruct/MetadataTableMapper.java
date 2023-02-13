package cn.datax.service.data.metadata.mapstruct;

import cn.datax.common.mapstruct.EntityMapper;
import cn.datax.service.data.metadata.api.dto.MetadataTableDto;
import cn.datax.service.data.metadata.api.entity.MetadataTableEntity;
import cn.datax.service.data.metadata.api.vo.MetadataTableVo;
import org.mapstruct.Mapper;

/**
 * <p>
 * 数据库表信息表 Mapper 实体映射
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-29
 */
@Mapper(componentModel = "spring")
public interface MetadataTableMapper extends EntityMapper<MetadataTableDto, MetadataTableEntity, MetadataTableVo> {

}
