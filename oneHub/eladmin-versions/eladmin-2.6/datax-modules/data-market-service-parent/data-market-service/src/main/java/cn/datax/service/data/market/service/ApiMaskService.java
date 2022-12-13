package cn.datax.service.data.market.service;

import cn.datax.common.base.BaseService;
import cn.datax.service.data.market.api.dto.ApiMaskDto;
import cn.datax.service.data.market.api.entity.ApiMaskEntity;

import java.util.List;

/**
 * <p>
 * 数据API脱敏信息表 服务类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
public interface ApiMaskService extends BaseService<ApiMaskEntity> {

    void saveApiMask(ApiMaskDto dataApiMask);

    void updateApiMask(ApiMaskDto dataApiMask);

    ApiMaskEntity getApiMaskById(String id);

    ApiMaskEntity getApiMaskByApiId(String apiId);

    void deleteApiMaskById(String id);

    void deleteApiMaskBatch(List<String> ids);
}
