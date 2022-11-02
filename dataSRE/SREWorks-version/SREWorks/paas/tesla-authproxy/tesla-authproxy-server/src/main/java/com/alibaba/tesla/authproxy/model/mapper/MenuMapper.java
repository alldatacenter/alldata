package com.alibaba.tesla.authproxy.model.mapper;

import com.alibaba.tesla.authproxy.model.MenuDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * <p>Title: MenuMapper.java<／p>
 * <p>Description: 菜单信息数据访问接口 <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
@Mapper
public interface MenuMapper {

    /**
     * 根据appId删除菜单
     *
     * @param appId
     * @return
     */
    int deleteByAppId(String appId);

    /**
     * 根据appId删除用户菜单关系
     *
     * @param appId
     * @return
     */
    int deleteUserMenusByAppId(String appId);

    /**
     * 添加菜单信息
     *
     * @param record
     * @return
     */
    int insert(MenuDO record);

    /**
     * 批量插入菜单
     *
     * @param menus
     * @return
     */
    int batchInsert(List<MenuDO> menus);

    /**
     * 根据主键更新菜单信息
     *
     * @param record
     * @return
     */
    int updateByPrimaryKey(MenuDO record);

    /**
     * 根据用户ID查询用户在某个应用下的有权限的菜单数据集合
     *
     * @param params 查询参数，包含用户ID和应用ID
     * @return
     */
    List<MenuDO> selectByUserId(Map<String, Object> params);

    /**
     * 查询角色在某个应用下的菜单数据
     *
     * @param params 应用ID：appId，角色名称：roleNames
     * @return
     */
    List<MenuDO> selectByRole(Map<String, Object> params);

    /**
     * 获取某个APP下的所有菜单
     *
     * @param appId
     * @return
     */
    List<MenuDO> selectByApp(String appId);

    /**
     * 根据菜单编码获取菜单
     *
     * @param menuCode
     * @return
     */
    MenuDO getMenuByCode(String menuCode);


}