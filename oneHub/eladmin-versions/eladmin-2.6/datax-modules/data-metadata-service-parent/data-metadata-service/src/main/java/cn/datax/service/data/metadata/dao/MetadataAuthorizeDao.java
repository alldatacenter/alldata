package cn.datax.service.data.metadata.dao;

import cn.datax.common.base.BaseDao;
import cn.datax.service.data.metadata.api.entity.MetadataAuthorizeEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 数据授权信息表 Mapper 接口
 * </p>
 *
 * @author yuwei
 * @since 2020-10-23
 */
@Mapper
public interface MetadataAuthorizeDao extends BaseDao<MetadataAuthorizeEntity> {

}
