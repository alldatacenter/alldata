package cn.datax.service.system.dao;

import cn.datax.service.system.api.entity.RoleMenuEntity;
import cn.datax.common.base.BaseDao;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author yuwei
 * @date 2022-09-11
 */
@Mapper
public interface RoleMenuDao extends BaseDao<RoleMenuEntity> {

    void insertBatch(List<RoleMenuEntity> list);

    void deleteByRoleId(String id);

    void deleteByRoleIds(List<String> ids);
}
