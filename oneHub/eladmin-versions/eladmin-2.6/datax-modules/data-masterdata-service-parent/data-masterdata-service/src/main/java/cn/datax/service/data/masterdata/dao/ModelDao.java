package cn.datax.service.data.masterdata.dao;

import cn.datax.common.base.BaseDao;
import cn.datax.service.data.masterdata.api.entity.ModelEntity;
import org.apache.ibatis.annotations.Mapper;

import java.io.Serializable;

/**
 * <p>
 * 主数据模型表 Mapper 接口
 * </p>
 *
 * @author yuwei
 * @since 2020-08-26
 */
@Mapper
public interface ModelDao extends BaseDao<ModelEntity> {

    @Override
    ModelEntity selectById(Serializable id);
}
