package cn.datax.service.system.dao;

import cn.datax.common.base.BaseDao;
import cn.datax.service.system.api.entity.UserRoleEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author yuwei
 * @date 2022-09-04
 */
@Mapper
public interface UserRoleDao extends BaseDao<UserRoleEntity> {

    void insertBatch(List<UserRoleEntity> list);

    void deleteByUserId(String id);

    void deleteByUserIds(List<String> ids);
}
