package cn.datax.service.data.metadata.mapstruct;

import cn.datax.common.mapstruct.EntityMapper;
import cn.datax.service.data.metadata.api.dto.MetadataChangeRecordDto;
import cn.datax.service.data.metadata.api.entity.MetadataChangeRecordEntity;
import cn.datax.service.data.metadata.api.vo.MetadataChangeRecordVo;
import org.mapstruct.Mapper;

/**
 * <p>
 * 元数据变更记录表 Mapper 实体映射
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-30
 */
@Mapper(componentModel = "spring")
public interface MetadataChangeRecordMapper extends EntityMapper<MetadataChangeRecordDto, MetadataChangeRecordEntity, MetadataChangeRecordVo> {

}
