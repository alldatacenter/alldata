package cn.datax.service.data.quality.service.impl;

import cn.datax.service.data.quality.api.entity.ScheduleLogEntity;
import cn.datax.service.data.quality.mapstruct.ScheduleLogMapper;
import cn.datax.service.data.quality.service.ScheduleLogService;
import cn.datax.service.data.quality.dao.ScheduleLogDao;
import cn.datax.common.base.BaseServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 数据质量监控任务日志信息表 服务实现类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-13
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ScheduleLogServiceImpl extends BaseServiceImpl<ScheduleLogDao, ScheduleLogEntity> implements ScheduleLogService {

    @Autowired
    private ScheduleLogDao scheduleLogDao;

    @Autowired
    private ScheduleLogMapper scheduleLogMapper;

    @Override
    public ScheduleLogEntity getScheduleLogById(String id) {
        ScheduleLogEntity scheduleLogEntity = super.getById(id);
        return scheduleLogEntity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteScheduleLogById(String id) {
        scheduleLogDao.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteScheduleLogBatch(List<String> ids) {
        scheduleLogDao.deleteBatchIds(ids);
    }
}
