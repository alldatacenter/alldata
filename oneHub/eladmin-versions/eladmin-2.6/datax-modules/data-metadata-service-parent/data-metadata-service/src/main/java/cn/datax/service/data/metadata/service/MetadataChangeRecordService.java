package cn.datax.service.data.metadata.service;

import cn.datax.service.data.metadata.api.entity.MetadataChangeRecordEntity;
import cn.datax.service.data.metadata.api.dto.MetadataChangeRecordDto;
import cn.datax.common.base.BaseService;

import java.util.List;

/**
 * <p>
 * 元数据变更记录表 服务类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-30
 */
public interface MetadataChangeRecordService extends BaseService<MetadataChangeRecordEntity> {

    MetadataChangeRecordEntity saveMetadataChangeRecord(MetadataChangeRecordDto metadataChangeRecord);

    MetadataChangeRecordEntity updateMetadataChangeRecord(MetadataChangeRecordDto metadataChangeRecord);

    MetadataChangeRecordEntity getMetadataChangeRecordById(String id);

    void deleteMetadataChangeRecordById(String id);

    void deleteMetadataChangeRecordBatch(List<String> ids);
}
