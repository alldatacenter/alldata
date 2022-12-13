package cn.datax.service.data.masterdata.service;

import cn.datax.service.data.masterdata.api.entity.ModelColumnEntity;
import cn.datax.service.data.masterdata.api.dto.ModelColumnDto;
import cn.datax.common.base.BaseService;

import java.util.List;

/**
 * <p>
 * 主数据模型列信息表 服务类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-26
 */
public interface ModelColumnService extends BaseService<ModelColumnEntity> {

    ModelColumnEntity saveModelColumn(ModelColumnDto modelColumn);

    ModelColumnEntity updateModelColumn(ModelColumnDto modelColumn);

    ModelColumnEntity getModelColumnById(String id);

    void deleteModelColumnById(String id);

    void deleteModelColumnBatch(List<String> ids);
}
