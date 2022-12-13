package cn.datax.service.data.metadata.service;

import cn.datax.service.data.metadata.api.entity.MetadataTableEntity;
import cn.datax.service.data.metadata.api.dto.MetadataTableDto;
import cn.datax.common.base.BaseService;
import cn.datax.service.data.metadata.api.query.MetadataTableQuery;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * <p>
 * 数据库表信息表 服务类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-29
 */
public interface MetadataTableService extends BaseService<MetadataTableEntity> {

    MetadataTableEntity saveMetadataTable(MetadataTableDto metadataTable);

    MetadataTableEntity updateMetadataTable(MetadataTableDto metadataTable);

    MetadataTableEntity getMetadataTableById(String id);

    void deleteMetadataTableById(String id);

    void deleteMetadataTableBatch(List<String> ids);

    List<MetadataTableEntity> getDataMetadataTableList(MetadataTableQuery metadataTableQuery);

    <E extends IPage<MetadataTableEntity>> E pageWithAuth(E page, Wrapper<MetadataTableEntity> queryWrapper);
}
