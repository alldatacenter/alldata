package cn.datax.service.system.service;

import cn.datax.common.base.BaseService;
import cn.datax.service.system.api.dto.LogDto;
import cn.datax.service.system.api.entity.LogEntity;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author yuwei
 * @date 2022-11-19
 */
public interface LogService extends BaseService<LogEntity> {

    void saveLog(LogDto log);

    LogEntity getLogById(String id);

    void deleteLogById(String id);

    void deleteLogBatch(List<String> ids);
}
