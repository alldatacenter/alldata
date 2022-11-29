package cn.datax.service.system.dao;

import cn.datax.common.base.BaseDao;
import cn.datax.service.system.api.entity.DictItemEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 字典项信息表 Mapper 接口
 * </p>
 *
 * @author yuwei
 * @since 2020-04-17
 */
@Mapper
public interface DictItemDao extends BaseDao<DictItemEntity> {

}
