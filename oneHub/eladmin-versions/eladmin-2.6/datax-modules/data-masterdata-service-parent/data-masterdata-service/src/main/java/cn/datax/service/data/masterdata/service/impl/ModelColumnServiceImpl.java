package cn.datax.service.data.masterdata.service.impl;

import cn.datax.service.data.masterdata.api.entity.ModelColumnEntity;
import cn.datax.service.data.masterdata.api.dto.ModelColumnDto;
import cn.datax.service.data.masterdata.service.ModelColumnService;
import cn.datax.service.data.masterdata.mapstruct.ModelColumnMapstruct;
import cn.datax.service.data.masterdata.dao.ModelColumnDao;
import cn.datax.common.base.BaseServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 主数据模型列信息表 服务实现类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-26
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ModelColumnServiceImpl extends BaseServiceImpl<ModelColumnDao, ModelColumnEntity> implements ModelColumnService {

    @Autowired
    private ModelColumnDao modelColumnDao;

    @Autowired
    private ModelColumnMapstruct modelColumnMapstruct;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelColumnEntity saveModelColumn(ModelColumnDto modelColumnDto) {
        ModelColumnEntity modelColumn = modelColumnMapstruct.toEntity(modelColumnDto);
        modelColumnDao.insert(modelColumn);
        return modelColumn;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelColumnEntity updateModelColumn(ModelColumnDto modelColumnDto) {
        ModelColumnEntity modelColumn = modelColumnMapstruct.toEntity(modelColumnDto);
        modelColumnDao.updateById(modelColumn);
        return modelColumn;
    }

    @Override
    public ModelColumnEntity getModelColumnById(String id) {
        ModelColumnEntity modelColumnEntity = super.getById(id);
        return modelColumnEntity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteModelColumnById(String id) {
        modelColumnDao.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteModelColumnBatch(List<String> ids) {
        modelColumnDao.deleteBatchIds(ids);
    }
}
