package cn.datax.service.system.dao;

import cn.datax.service.system.api.entity.MenuEntity;
import cn.datax.common.base.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
public interface MenuDao extends BaseDao<MenuEntity> {

    List<MenuEntity> selectMenuByRoleIds(@Param("roleIds") List<String> roleIds);

    List<MenuEntity> selectMenuByUserId(@Param("userId") String userId);
}
