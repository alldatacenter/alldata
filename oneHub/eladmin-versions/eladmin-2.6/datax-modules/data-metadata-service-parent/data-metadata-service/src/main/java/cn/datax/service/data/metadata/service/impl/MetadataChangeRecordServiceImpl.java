package cn.datax.service.data.metadata.service.impl;

import cn.datax.service.data.metadata.api.entity.MetadataChangeRecordEntity;
import cn.datax.service.data.metadata.api.dto.MetadataChangeRecordDto;
import cn.datax.service.data.metadata.service.MetadataChangeRecordService;
import cn.datax.service.data.metadata.mapstruct.MetadataChangeRecordMapper;
import cn.datax.service.data.metadata.dao.MetadataChangeRecordDao;
import cn.datax.common.base.BaseServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 元数据变更记录表 服务实现类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-30
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class MetadataChangeRecordServiceImpl extends BaseServiceImpl<MetadataChangeRecordDao, MetadataChangeRecordEntity> implements MetadataChangeRecordService {

    @Autowired
    private MetadataChangeRecordDao metadataChangeRecordDao;

    @Autowired
    private MetadataChangeRecordMapper metadataChangeRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MetadataChangeRecordEntity saveMetadataChangeRecord(MetadataChangeRecordDto metadataChangeRecordDto) {
        MetadataChangeRecordEntity metadataChangeRecord = metadataChangeRecordMapper.toEntity(metadataChangeRecordDto);
        metadataChangeRecordDao.insert(metadataChangeRecord);
        return metadataChangeRecord;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MetadataChangeRecordEntity updateMetadataChangeRecord(MetadataChangeRecordDto metadataChangeRecordDto) {
        MetadataChangeRecordEntity metadataChangeRecord = metadataChangeRecordMapper.toEntity(metadataChangeRecordDto);
        metadataChangeRecordDao.updateById(metadataChangeRecord);
        return metadataChangeRecord;
    }

    @Override
    public MetadataChangeRecordEntity getMetadataChangeRecordById(String id) {
        MetadataChangeRecordEntity metadataChangeRecordEntity = super.getById(id);
        return metadataChangeRecordEntity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMetadataChangeRecordById(String id) {
        metadataChangeRecordDao.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMetadataChangeRecordBatch(List<String> ids) {
        metadataChangeRecordDao.deleteBatchIds(ids);
    }
}
