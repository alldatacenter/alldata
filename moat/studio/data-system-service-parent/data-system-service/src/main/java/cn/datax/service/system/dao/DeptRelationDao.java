package cn.datax.service.system.dao;

import cn.datax.service.system.api.entity.DeptRelationEntity;
import cn.datax.common.base.BaseDao;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 * 部门关系表 Mapper 接口
 * </p>
 *
 * @author yuwei
 * @date 2022-11-22
 */
@Mapper
public interface DeptRelationDao extends BaseDao<DeptRelationEntity> {

    void insertBatch(List<DeptRelationEntity> list);

    void deleteByAncestor(String id);
}
