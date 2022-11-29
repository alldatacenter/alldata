package cn.datax.service.data.market.mapping.service;

import cn.datax.common.base.BaseService;
import cn.datax.service.data.market.api.dto.ApiLogDto;
import cn.datax.service.data.market.api.entity.ApiLogEntity;

import java.util.List;

public interface ApiLogService extends BaseService<ApiLogEntity> {

    ApiLogEntity saveApiLog(ApiLogDto apiLog);

    ApiLogEntity updateApiLog(ApiLogDto apiLog);

    ApiLogEntity getApiLogById(String id);

    void deleteApiLogById(String id);

    void deleteApiLogBatch(List<String> ids);
}
