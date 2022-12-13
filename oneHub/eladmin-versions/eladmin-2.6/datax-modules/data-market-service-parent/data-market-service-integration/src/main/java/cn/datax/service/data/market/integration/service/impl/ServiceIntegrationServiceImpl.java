package cn.datax.service.data.market.integration.service.impl;

import cn.datax.common.utils.MD5Util;
import cn.datax.common.utils.SecurityUtil;
import cn.datax.service.data.market.api.entity.ServiceIntegrationEntity;
import cn.datax.service.data.market.api.dto.ServiceIntegrationDto;
import cn.datax.service.data.market.api.vo.ApiHeader;
import cn.datax.service.data.market.api.vo.ServiceHeader;
import cn.datax.service.data.market.integration.service.ServiceIntegrationService;
import cn.datax.service.data.market.integration.mapstruct.ServiceIntegrationMapper;
import cn.datax.service.data.market.integration.dao.ServiceIntegrationDao;
import cn.datax.common.base.BaseServiceImpl;
import cn.datax.service.data.market.integration.utils.SerialUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 服务集成表 服务实现类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-20
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ServiceIntegrationServiceImpl extends BaseServiceImpl<ServiceIntegrationDao, ServiceIntegrationEntity> implements ServiceIntegrationService {

    @Autowired
    private ServiceIntegrationDao serviceIntegrationDao;

    @Autowired
    private ServiceIntegrationMapper serviceIntegrationMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ServiceIntegrationEntity saveServiceIntegration(ServiceIntegrationDto serviceIntegrationDto) {
        ServiceIntegrationEntity serviceIntegration = serviceIntegrationMapper.toEntity(serviceIntegrationDto);
        String serialNo = SerialUtil.getSerialNo(3);
        serviceIntegration.setServiceNo(serialNo);
        serviceIntegrationDao.insert(serviceIntegration);
        return serviceIntegration;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ServiceIntegrationEntity updateServiceIntegration(ServiceIntegrationDto serviceIntegrationDto) {
        ServiceIntegrationEntity serviceIntegration = serviceIntegrationMapper.toEntity(serviceIntegrationDto);
        serviceIntegrationDao.updateById(serviceIntegration);
        return serviceIntegration;
    }

    @Override
    public ServiceIntegrationEntity getServiceIntegrationById(String id) {
        ServiceIntegrationEntity serviceIntegrationEntity = super.getById(id);
        return serviceIntegrationEntity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteServiceIntegrationById(String id) {
        serviceIntegrationDao.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteServiceIntegrationBatch(List<String> ids) {
        serviceIntegrationDao.deleteBatchIds(ids);
    }

    @Override
    public Map<String, Object> getServiceIntegrationDetailById(String id) {
        ServiceIntegrationEntity serviceIntegrationEntity = super.getById(id);
        ServiceHeader serviceHeader = new ServiceHeader();
        MD5Util mt = null;
        try {
            mt = MD5Util.getInstance();
            serviceHeader.setServiceKey(mt.encode(id));
            serviceHeader.setSecretKey(mt.encode(SecurityUtil.getUserId()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        Map<String, Object> map = new HashMap<>(2);
        map.put("data", serviceIntegrationMapper.toVO(serviceIntegrationEntity));
        map.put("header", serviceHeader);
        return map;
    }
}
