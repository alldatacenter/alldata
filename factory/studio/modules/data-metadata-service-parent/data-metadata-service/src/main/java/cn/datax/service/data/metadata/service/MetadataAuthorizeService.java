package cn.datax.service.data.metadata.service;

import cn.datax.service.data.metadata.api.dto.MetadataAuthorizeDto;
import cn.datax.service.data.metadata.api.entity.MetadataAuthorizeEntity;
import cn.datax.common.base.BaseService;

import java.util.List;

/**
 * <p>
 * 数据授权信息表 服务类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-23
 */
public interface MetadataAuthorizeService extends BaseService<MetadataAuthorizeEntity> {

    List<String> getAuthorizedMetadata(String id);

    void metadataAuthorize(MetadataAuthorizeDto metadataAuthorizeDto);

    void refreshCache();
}
