package cn.datax.service.quartz.service;

import cn.datax.service.quartz.api.entity.QrtzJobEntity;
import cn.datax.service.quartz.api.dto.QrtzJobDto;
import cn.datax.common.base.BaseService;

/**
 * <p>
 * 定时任务信息表 服务类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
public interface QrtzJobService extends BaseService<QrtzJobEntity> {

    void saveQrtzJob(QrtzJobDto qrtzJob);

    void updateQrtzJob(QrtzJobDto qrtzJob);

    QrtzJobEntity getQrtzJobById(String id);

    void deleteQrtzJobById(String id);

    void pauseById(String id);

    void resumeById(String id);

    void runById(String id);
}
