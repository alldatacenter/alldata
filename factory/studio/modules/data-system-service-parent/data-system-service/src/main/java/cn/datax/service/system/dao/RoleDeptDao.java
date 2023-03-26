package cn.datax.service.system.dao;

import cn.datax.service.system.api.entity.RoleDeptEntity;
import cn.datax.common.base.BaseDao;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 * 角色和部门关联表 Mapper 接口
 * </p>
 *
 * @author yuwei
 * @date 2022-11-22
 */
@Mapper
public interface RoleDeptDao extends BaseDao<RoleDeptEntity> {

    void insertBatch(List<RoleDeptEntity> list);

    void deleteByRoleId(String id);

    void deleteByRoleIds(List<String> ids);
}
