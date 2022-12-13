package cn.datax.service.data.market.integration.mapstruct;

import cn.datax.common.mapstruct.EntityMapper;
import cn.datax.service.data.market.api.dto.ServiceLogDto;
import cn.datax.service.data.market.api.entity.ServiceLogEntity;
import cn.datax.service.data.market.api.vo.ServiceLogVo;
import org.mapstruct.Mapper;

/**
 * <p>
 * 服务集成调用日志表 Mapper 实体映射
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-20
 */
@Mapper(componentModel = "spring")
public interface ServiceLogMapper extends EntityMapper<ServiceLogDto, ServiceLogEntity, ServiceLogVo> {

}
