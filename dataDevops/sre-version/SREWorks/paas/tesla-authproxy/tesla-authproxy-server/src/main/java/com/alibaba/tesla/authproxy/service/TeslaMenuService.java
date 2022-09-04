package com.alibaba.tesla.authproxy.service;

import com.alibaba.tesla.authproxy.ApplicationException;
import com.alibaba.tesla.authproxy.model.MenuDO;
import com.alibaba.tesla.authproxy.model.vo.MenuDoTreeVO;

import java.util.List;
import java.util.Map;

/**
 * <p>Title: TeslaMenuService.java<／p>
 * <p>Description: 菜单权限服务接口 <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年4月17日
 */
public interface TeslaMenuService {

    /**
     * 初始化应用下的菜单数据
     *
     * @param appId  应用ID
     * @param userId 用户ID
     * @param menus  菜单数据
     * @return
     * @throws ApplicationException
     */
    @Deprecated
    void initMenuByApp(String appId, long userId, List<MenuDO> menus) throws ApplicationException;

    /**
     * 初始化应用下菜单并授权所有菜单给默认角色
     *
     * @param appId
     * @param userId
     * @param menus
     * @throws ApplicationException
     */
    void initMenuByAppNew(String appId, long userId, List<MenuDO> menus) throws ApplicationException;

    /**
     * 根据用户ID，加载用户有权的菜单数据，返回一个树形多级结构菜单数据
     *
     * @param userId 用户ID
     * @param appId  应用ID
     * @return
     * @throws ApplicationException
     */
    List<MenuDoTreeVO> listMenuByUser(long userId, String appId) throws ApplicationException;


    /**
     * 根据当前用户的角色加载菜单数据，有多个角色的用户取菜单数据并集
     *
     * @param roleNames 用户拥有的角色名称集合
     * @param appId     应用ID
     * @return
     * @throws ApplicationException
     */
    List<MenuDoTreeVO> listMenuByRole(List<String> roleNames, String appId) throws ApplicationException;

    /**
     * 加载某个应用下的所有菜单
     *
     * @param appId
     * @return
     * @throws ApplicationException
     */
    List<MenuDoTreeVO> listMenuByApp(String appId) throws ApplicationException;

    /**
     * 添加新的菜单
     *
     * @param record 菜单信息
     * @param appId  应用ID
     * @return
     * @throws ApplicationException
     */
    int insert(MenuDO record, String appId) throws ApplicationException;

    /**
     * 批量插入菜单数据
     *
     * @param menus
     * @return
     * @throws ApplicationException
     */
    int batchInsert(List<MenuDO> menus) throws ApplicationException;

    /**
     * 同步菜单
     *
     * @param menus
     * @return
     * @throws ApplicationException
     */
    Map<String, Object> syncMenus(List<MenuDO> menus) throws ApplicationException;

}
