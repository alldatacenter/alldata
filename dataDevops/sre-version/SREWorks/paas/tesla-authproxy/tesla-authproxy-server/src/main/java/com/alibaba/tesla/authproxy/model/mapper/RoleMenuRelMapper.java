package com.alibaba.tesla.authproxy.model.mapper;

import com.alibaba.tesla.authproxy.model.RoleMenuRelDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>Title: RoleMenuRelMapper.java<／p>
 * <p>Description: 角色菜单关系数据访问接口 <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
@Mapper
public interface RoleMenuRelMapper {

    /**
     * 根据应用ID该应用下的角色菜单关系
     *
     * @param appId 应用ID
     * @return
     */
    int deleteByApp(String appId);

    /**
     * 批量插入角色菜单关系
     *
     * @param roleMenuRels
     * @return
     */
    int batchInsert(List<RoleMenuRelDO> roleMenuRels);


}