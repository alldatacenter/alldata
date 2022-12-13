package cn.datax.service.quartz.service.impl;

import cn.datax.service.quartz.api.entity.QrtzJobLogEntity;
import cn.datax.service.quartz.api.dto.QrtzJobLogDto;
import cn.datax.service.quartz.service.QrtzJobLogService;
import cn.datax.service.quartz.mapstruct.QrtzJobLogMapper;
import cn.datax.service.quartz.dao.QrtzJobLogDao;
import cn.datax.common.base.BaseServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 定时任务日志信息表 服务实现类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class QrtzJobLogServiceImpl extends BaseServiceImpl<QrtzJobLogDao, QrtzJobLogEntity> implements QrtzJobLogService {

    @Autowired
    private QrtzJobLogDao qrtzJobLogDao;

    @Autowired
    private QrtzJobLogMapper qrtzJobLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveQrtzJobLog(QrtzJobLogDto qrtzJobLogDto) {
        QrtzJobLogEntity qrtzJobLog = qrtzJobLogMapper.toEntity(qrtzJobLogDto);
        qrtzJobLogDao.insert(qrtzJobLog);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQrtzJobLog(QrtzJobLogDto qrtzJobLogDto) {
        QrtzJobLogEntity qrtzJobLog = qrtzJobLogMapper.toEntity(qrtzJobLogDto);
        qrtzJobLogDao.updateById(qrtzJobLog);
    }

    @Override
    public QrtzJobLogEntity getQrtzJobLogById(String id) {
        QrtzJobLogEntity qrtzJobLogEntity = super.getById(id);
        return qrtzJobLogEntity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteQrtzJobLogById(String id) {
        qrtzJobLogDao.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteQrtzJobLogBatch(List<String> ids) {
        qrtzJobLogDao.deleteBatchIds(ids);
    }
}
