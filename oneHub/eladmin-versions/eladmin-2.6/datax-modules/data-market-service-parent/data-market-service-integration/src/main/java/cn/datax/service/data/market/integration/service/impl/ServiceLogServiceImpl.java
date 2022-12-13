package cn.datax.service.data.market.integration.service.impl;

import cn.datax.service.data.market.api.entity.ServiceLogEntity;
import cn.datax.service.data.market.api.dto.ServiceLogDto;
import cn.datax.service.data.market.integration.service.ServiceLogService;
import cn.datax.service.data.market.integration.mapstruct.ServiceLogMapper;
import cn.datax.service.data.market.integration.dao.ServiceLogDao;
import cn.datax.common.base.BaseServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 服务集成调用日志表 服务实现类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-20
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ServiceLogServiceImpl extends BaseServiceImpl<ServiceLogDao, ServiceLogEntity> implements ServiceLogService {

    @Autowired
    private ServiceLogDao serviceLogDao;

    @Autowired
    private ServiceLogMapper serviceLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ServiceLogEntity saveServiceLog(ServiceLogDto serviceLogDto) {
        ServiceLogEntity serviceLog = serviceLogMapper.toEntity(serviceLogDto);
        serviceLogDao.insert(serviceLog);
        return serviceLog;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ServiceLogEntity updateServiceLog(ServiceLogDto serviceLogDto) {
        ServiceLogEntity serviceLog = serviceLogMapper.toEntity(serviceLogDto);
        serviceLogDao.updateById(serviceLog);
        return serviceLog;
    }

    @Override
    public ServiceLogEntity getServiceLogById(String id) {
        ServiceLogEntity serviceLogEntity = super.getById(id);
        return serviceLogEntity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteServiceLogById(String id) {
        serviceLogDao.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteServiceLogBatch(List<String> ids) {
        serviceLogDao.deleteBatchIds(ids);
    }
}
