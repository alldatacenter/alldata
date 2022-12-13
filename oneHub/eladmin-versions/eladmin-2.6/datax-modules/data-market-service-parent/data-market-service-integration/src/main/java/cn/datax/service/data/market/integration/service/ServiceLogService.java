package cn.datax.service.data.market.integration.service;

import cn.datax.service.data.market.api.entity.ServiceLogEntity;
import cn.datax.service.data.market.api.dto.ServiceLogDto;
import cn.datax.common.base.BaseService;

import java.util.List;

/**
 * <p>
 * 服务集成调用日志表 服务类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-20
 */
public interface ServiceLogService extends BaseService<ServiceLogEntity> {

    ServiceLogEntity saveServiceLog(ServiceLogDto serviceLog);

    ServiceLogEntity updateServiceLog(ServiceLogDto serviceLog);

    ServiceLogEntity getServiceLogById(String id);

    void deleteServiceLogById(String id);

    void deleteServiceLogBatch(List<String> ids);
}
