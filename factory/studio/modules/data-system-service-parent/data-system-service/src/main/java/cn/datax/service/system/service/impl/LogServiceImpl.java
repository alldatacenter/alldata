package cn.datax.service.system.service.impl;

import cn.datax.service.system.api.dto.LogDto;
import cn.datax.service.system.api.entity.LogEntity;
import cn.datax.service.system.dao.LogDao;
import cn.datax.service.system.mapstruct.LogMapper;
import cn.datax.service.system.service.LogService;
import cn.datax.common.base.BaseServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author yuwei
 * @date 2022-11-19
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class LogServiceImpl extends BaseServiceImpl<LogDao, LogEntity> implements LogService {

    @Autowired
    private LogDao logDao;
    @Autowired
    private LogMapper logMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveLog(LogDto logDto) {
        LogEntity log = logMapper.toEntity(logDto);
        logDao.insert(log);
    }

    @Override
    public LogEntity getLogById(String id) {
        LogEntity logEntity = super.getById(id);
        return logEntity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLogById(String id) {
        logDao.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLogBatch(List<String> ids) {
        logDao.deleteBatchIds(ids);
    }
}
