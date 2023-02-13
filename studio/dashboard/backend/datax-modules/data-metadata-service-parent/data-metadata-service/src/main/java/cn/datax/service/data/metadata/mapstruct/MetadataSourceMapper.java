package cn.datax.service.data.metadata.mapstruct;

import cn.datax.common.mapstruct.EntityMapper;
import cn.datax.service.data.metadata.api.dto.MetadataSourceDto;
import cn.datax.service.data.metadata.api.entity.MetadataSourceEntity;
import cn.datax.service.data.metadata.api.vo.MetadataSourceVo;
import org.mapstruct.Mapper;

/**
 * <p>
 * 数据源信息表 Mapper 实体映射
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
@Mapper(componentModel = "spring")
public interface MetadataSourceMapper extends EntityMapper<MetadataSourceDto, MetadataSourceEntity, MetadataSourceVo> {

}
