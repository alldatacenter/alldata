package cn.datax.service.quartz.service;

import cn.datax.service.quartz.api.entity.QrtzJobLogEntity;
import cn.datax.service.quartz.api.dto.QrtzJobLogDto;
import cn.datax.common.base.BaseService;

import java.util.List;

/**
 * <p>
 * 定时任务日志信息表 服务类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
public interface QrtzJobLogService extends BaseService<QrtzJobLogEntity> {

    void saveQrtzJobLog(QrtzJobLogDto qrtzJobLog);

    void updateQrtzJobLog(QrtzJobLogDto qrtzJobLog);

    QrtzJobLogEntity getQrtzJobLogById(String id);

    void deleteQrtzJobLogById(String id);

    void deleteQrtzJobLogBatch(List<String> ids);
}
