package cn.datax.service.data.quality.service;

import cn.datax.service.data.quality.api.entity.ScheduleJobEntity;
import cn.datax.common.base.BaseService;

/**
 * <p>
 * 数据质量监控任务信息表 服务类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-29
 */
public interface ScheduleJobService extends BaseService<ScheduleJobEntity> {

    ScheduleJobEntity getScheduleJobById(String id);

    void pauseScheduleJobById(String id);

    void resumeScheduleJobById(String id);

	void runScheduleJobById(String id);
}
