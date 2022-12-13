package cn.datax.service.data.market.integration.mapstruct;

import cn.datax.common.mapstruct.EntityMapper;
import cn.datax.service.data.market.api.dto.ServiceIntegrationDto;
import cn.datax.service.data.market.api.entity.ServiceIntegrationEntity;
import cn.datax.service.data.market.api.vo.ServiceIntegrationVo;
import org.mapstruct.Mapper;

/**
 * <p>
 * 服务集成表 Mapper 实体映射
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-20
 */
@Mapper(componentModel = "spring")
public interface ServiceIntegrationMapper extends EntityMapper<ServiceIntegrationDto, ServiceIntegrationEntity, ServiceIntegrationVo> {

}
