package com.alibaba.tesla.authproxy.service.impl;

import com.alibaba.tesla.authproxy.ApplicationException;
import com.alibaba.tesla.authproxy.model.mapper.AppMapper;
import com.alibaba.tesla.authproxy.model.mapper.RoleMenuRelMapper;
import com.alibaba.tesla.authproxy.model.mapper.MenuMapper;
import com.alibaba.tesla.authproxy.model.AppDO;
import com.alibaba.tesla.authproxy.model.RoleMenuRelDO;
import com.alibaba.tesla.authproxy.model.MenuDO;
import com.alibaba.tesla.authproxy.model.vo.MenuDoTreeVO;
import com.alibaba.tesla.authproxy.service.TeslaMenuService;
import com.alibaba.tesla.common.utils.TeslaResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * <p>Title: TeslaMenuServiceImpl.java<／p>
 * <p>Description: 菜单服务实现 <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
@Service
@Slf4j
public class TeslaMenuServiceImpl implements TeslaMenuService {

    /**
     * 菜单信息数据访问接口
     */
    @Autowired
    MenuMapper menuMapper;

    @Autowired
    RoleMenuRelMapper roleMenuRelMapper;

    @Autowired
    AppMapper appMapper;

    /**
     * 初始化应用下的菜单
     *
     * @param appId  应用ID
     * @param userId 用户ID
     * @param menus  菜单数据
     * @return
     * @throws ApplicationException
     */
    @Override
    @Transactional
    @Deprecated
    public void initMenuByApp(String appId, long userId, List<MenuDO> menus) throws ApplicationException {

        //批量删除appId下现有菜单和用户的关系
        int umr = menuMapper.deleteUserMenusByAppId(appId);

        //批量删除appId下现有菜单数据
        int m = menuMapper.deleteByAppId(appId);

        //批量添加现有菜单
        int im = menuMapper.batchInsert(menus);

        log.info("删除用户菜单关系[{}],删除菜单[{}],添加菜单[{}]", umr, m, im);
    }

    /**
     * 初始化菜单并授权给默认角色
     *
     * @param appId  应用ID
     * @param userId 用户ID
     * @param menus  初始化菜单数据
     * @throws ApplicationException
     */
    @Override
    @Transactional
    public void initMenuByAppNew(String appId, long userId, List<MenuDO> menus) throws ApplicationException {

        AppDO appDo = appMapper.getByAppId(appId);
        if (null == appDo) {
            log.error("初始化失败:{}应用不存在", appId);
            throw new ApplicationException(TeslaResult.FAILURE, "error.menu.init.appempty");
        }

        //批量删除appId下现有菜单和用户的关系
        int rmr = roleMenuRelMapper.deleteByApp(appId);

        //批量删除appId下现有菜单数据
        int m = menuMapper.deleteByAppId(appId);

        //批量添加现有菜单
        int im = menuMapper.batchInsert(menus);

        List<RoleMenuRelDO> roleMenuRels = new ArrayList<RoleMenuRelDO>();
        if (null == appDo.getAdminRoleName() || appDo.getAdminRoleName().length() == 0) {
            log.warn("应用{}没有初始化角色,不能给角色初始化授权菜单", appId);
            return;
        }

        //批量插入角色菜单关系
        for (MenuDO menu : menus) {
            RoleMenuRelDO roleMenuRel = new RoleMenuRelDO();
            roleMenuRel.setAppId(appId);
            roleMenuRel.setMenuCode(menu.getMenuCode());
            roleMenuRel.setRoleName(appDo.getAdminRoleName());
            roleMenuRel.setGmtCreate(new Date());
            roleMenuRel.setMemo(menu.getMenuTitle());
            roleMenuRels.add(roleMenuRel);
        }
        int irmr = roleMenuRelMapper.batchInsert(roleMenuRels);

        log.info("初始化成功，删除角色菜单关系[{}],删除菜单[{}],添加菜单[{}],添加角色菜单关系[{}]", rmr, m, im, irmr);
    }

    /**
     * 查询菜单树形结构数据
     */
    @Override
    public List<MenuDoTreeVO> listMenuByUser(long userId, String appId) throws ApplicationException {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("userId", userId);
        params.put("appId", appId);
        List<MenuDO> menuDos = menuMapper.selectByUserId(params);

        List<MenuDoTreeVO> menus = new ArrayList<MenuDoTreeVO>();

        for (MenuDO menu : menuDos) {
            if (null == menu.getParentCode()) {
                MenuDoTreeVO root = new MenuDoTreeVO();
                root.setId(menu.getId().longValue());
                root.setCode(menu.getMenuCode());
                root.setName(menu.getMenuName());
                root.setTitle(menu.getMenuTitle());
                root.setSref(menu.getMenuUrl());
                root.setIcon(menu.getIcon());
                if (menu.getIsLeaf().intValue() == 0) {
                    root.setChildren(getSubMenus(root, menuDos));
                }
                menus.add(root);
            }
        }
        return menus;
    }

    @Override
    public List<MenuDoTreeVO> listMenuByRole(List<String> roleNames, String appId) throws ApplicationException {

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("roleNames", roleNames);
        params.put("appId", appId);
        List<MenuDO> menuDos = menuMapper.selectByRole(params);

        List<MenuDoTreeVO> menus = new ArrayList<MenuDoTreeVO>();

        for (MenuDO menu : menuDos) {
            if (null == menu.getParentCode()) {
                MenuDoTreeVO root = new MenuDoTreeVO();
                root.setId(menu.getId().longValue());
                root.setCode(menu.getMenuCode());
                root.setName(menu.getMenuName());
                root.setTitle(menu.getMenuTitle());
                root.setSref(menu.getMenuUrl());
                root.setIcon(menu.getIcon());
                root.setHeaderTitleSet(menu.getHeaderTitleSet());
                if (menu.getIsLeaf().intValue() == 0) {
                    root.setChildren(getSubMenus(root, menuDos));
                }
                menus.add(root);
            }
        }
        return menus;
    }

    @Override
    public List<MenuDoTreeVO> listMenuByApp(String appId) throws ApplicationException {
        List<MenuDO> menuDos = menuMapper.selectByApp(appId);

        List<MenuDoTreeVO> menus = new ArrayList<MenuDoTreeVO>();

        for (MenuDO menu : menuDos) {
            if (null == menu.getParentCode()) {
                MenuDoTreeVO root = new MenuDoTreeVO();
                root.setId(menu.getId().longValue());
                root.setCode(menu.getMenuCode());
                root.setName(menu.getMenuName());
                root.setTitle(menu.getMenuTitle());
                root.setSref(menu.getMenuUrl());
                root.setIcon(menu.getIcon());
                root.setHeaderTitleSet(menu.getHeaderTitleSet());
                if (menu.getIsLeaf().intValue() == 0) {
                    root.setChildren(getSubMenus(root, menuDos));
                }
                menus.add(root);
            }
        }
        return menus;
    }

    /**
     * 加载子菜单
     *
     * @param parent
     * @param menus
     * @return
     */
    private List<MenuDoTreeVO> getSubMenus(MenuDoTreeVO parent, List<MenuDO> menus) {
        List<MenuDoTreeVO> subs = new ArrayList<MenuDoTreeVO>();
        for (MenuDO menu : menus) {
            if (null != menu.getParentCode() && menu.getParentCode().equals(parent.getCode())) {
                MenuDoTreeVO menuNode = new MenuDoTreeVO();
                menuNode.setId(menu.getId().longValue());
                menuNode.setCode(menu.getMenuCode());
                menuNode.setName(menu.getMenuName());
                menuNode.setTitle(menu.getMenuTitle());
                menuNode.setSref(menu.getMenuUrl());
                menuNode.setIcon(menu.getIcon());
                menuNode.setHeaderTitleSet(menu.getHeaderTitleSet());
                if (menu.getIsLeaf().intValue() == 0) {
                    List<MenuDO> restMenus = menus;
                    menus = null;
                    menuNode.setChildren(getSubMenus(menuNode, restMenus));
                }
                subs.add(menuNode);
            }
        }
        return subs;
    }

    @Override
    public int insert(MenuDO record, String appId) throws ApplicationException {
        return 0;
    }

    @Override
    public int batchInsert(List<MenuDO> menus) throws ApplicationException {
        try {
            return menuMapper.batchInsert(menus);
        } catch (Exception e) {
            log.error("批量插入菜单失败", e);
            throw new ApplicationException(TeslaResult.FAILURE, "error.menu.batchInsert");
        }
    }

    @Override
    public Map<String, Object> syncMenus(List<MenuDO> menus) throws ApplicationException {
        for (MenuDO menu : menus) {
            MenuDO m = menuMapper.getMenuByCode(menu.getMenuCode());
            if (null != m) {
                m.setMenuName(menu.getMenuName());
                m.setIsEnable(menu.getIsEnable());
                m.setMenuTitle(menu.getMenuTitle());
                m.setMenuUrl(menu.getMenuUrl());
                m.setParentCode(menu.getParentCode());
                m.setIcon(menu.getIcon());
            }
            menuMapper.updateByPrimaryKey(m);
        }
        return null;
    }
}