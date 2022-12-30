package cn.datax.service.data.metadata.service;

import cn.datax.service.data.metadata.api.dto.MetadataColumnDto;
import cn.datax.common.base.BaseService;
import cn.datax.service.data.metadata.api.entity.MetadataColumnEntity;
import cn.datax.service.data.metadata.api.query.MetadataColumnQuery;
import cn.datax.service.data.metadata.api.vo.MetadataTreeVo;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * <p>
 * 元数据信息表 服务类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-29
 */
public interface MetadataColumnService extends BaseService<MetadataColumnEntity> {

    MetadataColumnEntity saveMetadataColumn(MetadataColumnDto metadataColumn);

    MetadataColumnEntity updateMetadataColumn(MetadataColumnDto metadataColumn);

    MetadataColumnEntity getMetadataColumnById(String id);

    void deleteMetadataColumnById(String id);

    void deleteMetadataColumnBatch(List<String> ids);

    List<MetadataTreeVo> getDataMetadataTree(String level, MetadataColumnQuery metadataColumnQuery);

    List<MetadataColumnEntity> getDataMetadataColumnList(MetadataColumnQuery metadataColumnQuery);

    <E extends IPage<MetadataColumnEntity>> E pageWithAuth(E page, Wrapper<MetadataColumnEntity> queryWrapper);
}
