package cn.datax.service.quartz.service.impl;

import cn.datax.common.core.DataConstant;
import cn.datax.service.quartz.api.entity.QrtzJobEntity;
import cn.datax.service.quartz.api.dto.QrtzJobDto;
import cn.datax.service.quartz.quartz.utils.ScheduleUtil;
import cn.datax.service.quartz.service.QrtzJobService;
import cn.datax.service.quartz.mapstruct.QrtzJobMapper;
import cn.datax.service.quartz.dao.QrtzJobDao;
import cn.datax.common.base.BaseServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 定时任务信息表 服务实现类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class QrtzJobServiceImpl extends BaseServiceImpl<QrtzJobDao, QrtzJobEntity> implements QrtzJobService {

    @Autowired
    private QrtzJobDao qrtzJobDao;

    @Autowired
    private QrtzJobMapper qrtzJobMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveQrtzJob(QrtzJobDto qrtzJobDto) {
        QrtzJobEntity qrtzJob = qrtzJobMapper.toEntity(qrtzJobDto);
        qrtzJobDao.insert(qrtzJob);
        ScheduleUtil.createScheduleJob(qrtzJob);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQrtzJob(QrtzJobDto qrtzJobDto) {
        QrtzJobEntity qrtzJob = qrtzJobMapper.toEntity(qrtzJobDto);
        qrtzJobDao.updateById(qrtzJob);
        ScheduleUtil.updateScheduleJob(qrtzJob);
    }

    @Override
    public QrtzJobEntity getQrtzJobById(String id) {
        QrtzJobEntity qrtzJobEntity = super.getById(id);
        return qrtzJobEntity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteQrtzJobById(String id) {
        ScheduleUtil.deleteJob(id);
        qrtzJobDao.deleteById(id);
    }

    @Override
    public void pauseById(String id) {
        QrtzJobEntity job = getQrtzJobById(id);
        job.setStatus(DataConstant.EnableState.DISABLE.getKey());
        super.updateById(job);
        ScheduleUtil.pauseJob(id);
    }

    @Override
    public void resumeById(String id) {
        QrtzJobEntity job = getQrtzJobById(id);
        job.setStatus(DataConstant.EnableState.ENABLE.getKey());
        super.updateById(job);
        ScheduleUtil.resumeJob(id);
    }

    @Override
    public void runById(String id) {
        QrtzJobEntity job = getQrtzJobById(id);
        ScheduleUtil.runJob(job);
    }
}
