package cn.datax.service.data.masterdata.service;

import cn.datax.service.data.masterdata.api.entity.ModelEntity;
import cn.datax.service.data.masterdata.api.dto.ModelDto;
import cn.datax.common.base.BaseService;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 主数据模型表 服务类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-26
 */
public interface ModelService extends BaseService<ModelEntity> {

    ModelEntity saveModel(ModelDto model);

    ModelEntity updateModel(ModelDto model);

    ModelEntity getModelById(String id);

    void deleteModelById(String id);

    void deleteModelBatch(List<String> ids);

    void createTable(String id);

    void dropTable(String id);

    Map<String, Object> getTableParamById(String id);

    Map<String, Object> getFormParamById(String id);

    void submitModelById(String id);
}
