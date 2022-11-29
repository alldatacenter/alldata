package cn.datax.service.data.metadata.mapstruct;

import cn.datax.common.mapstruct.EntityMapper;
import cn.datax.service.data.metadata.api.dto.MetadataAuthorizeDto;
import cn.datax.service.data.metadata.api.entity.MetadataAuthorizeEntity;
import cn.datax.service.data.metadata.api.vo.MetadataAuthorizeVo;
import org.mapstruct.Mapper;

/**
 * <p>
 * 数据授权信息表 Mapper 实体映射
 * </p>
 *
 * @author yuwei
 * @since 2020-10-23
 */
@Mapper(componentModel = "spring")
public interface MetadataAuthorizeMapper extends EntityMapper<MetadataAuthorizeDto, MetadataAuthorizeEntity, MetadataAuthorizeVo> {

}
