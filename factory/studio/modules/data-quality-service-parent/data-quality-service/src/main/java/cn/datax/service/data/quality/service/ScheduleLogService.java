package cn.datax.service.data.quality.service;

import cn.datax.service.data.quality.api.entity.ScheduleLogEntity;
import cn.datax.common.base.BaseService;

import java.util.List;

/**
 * <p>
 * 数据质量监控任务日志信息表 服务类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-13
 */
public interface ScheduleLogService extends BaseService<ScheduleLogEntity> {

    ScheduleLogEntity getScheduleLogById(String id);

    void deleteScheduleLogById(String id);

    void deleteScheduleLogBatch(List<String> ids);
}
