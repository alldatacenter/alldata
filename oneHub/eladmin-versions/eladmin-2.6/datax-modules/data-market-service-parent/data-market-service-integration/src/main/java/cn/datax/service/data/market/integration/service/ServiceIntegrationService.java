package cn.datax.service.data.market.integration.service;

import cn.datax.service.data.market.api.entity.ServiceIntegrationEntity;
import cn.datax.service.data.market.api.dto.ServiceIntegrationDto;
import cn.datax.common.base.BaseService;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 服务集成表 服务类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-20
 */
public interface ServiceIntegrationService extends BaseService<ServiceIntegrationEntity> {

    ServiceIntegrationEntity saveServiceIntegration(ServiceIntegrationDto serviceIntegration);

    ServiceIntegrationEntity updateServiceIntegration(ServiceIntegrationDto serviceIntegration);

    ServiceIntegrationEntity getServiceIntegrationById(String id);

    void deleteServiceIntegrationById(String id);

    void deleteServiceIntegrationBatch(List<String> ids);

    Map<String, Object> getServiceIntegrationDetailById(String id);
}
