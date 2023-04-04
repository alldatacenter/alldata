package com.vince.xq.project.system.menu.controller;

import java.util.List;

import com.vince.xq.common.constant.UserConstants;
import com.vince.xq.common.utils.security.AuthorizationUtils;
import com.vince.xq.framework.aspectj.lang.annotation.Log;
import com.vince.xq.framework.aspectj.lang.enums.BusinessType;
import com.vince.xq.framework.web.controller.BaseController;
import com.vince.xq.framework.web.domain.AjaxResult;
import com.vince.xq.framework.web.domain.Ztree;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.vince.xq.project.system.menu.domain.Menu;
import com.vince.xq.project.system.menu.service.IMenuService;
import com.vince.xq.project.system.role.domain.Role;

/**
 * 菜单信息
 * 
 * @author ruoyi
 */
@Controller
@RequestMapping("/system/menu")
public class MenuController extends BaseController
{
    private String prefix = "system/menu";

    @Autowired
    private IMenuService menuService;

    @RequiresPermissions("system:menu:view")
    @GetMapping()
    public String menu()
    {
        return prefix + "/menu";
    }

    @RequiresPermissions("system:menu:list")
    @PostMapping("/list")
    @ResponseBody
    public List<Menu> list(Menu menu)
    {
        List<Menu> menuList = menuService.selectMenuList(menu);
        return menuList;
    }

    /**
     * 删除菜单
     */
    @Log(title = "菜单管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("system:menu:remove")
    @GetMapping("/remove/{menuId}")
    @ResponseBody
    public AjaxResult remove(@PathVariable("menuId") Long menuId)
    {
        if (menuService.selectCountMenuByParentId(menuId) > 0)
        {
            return AjaxResult.warn("存在子菜单,不允许删除");
        }
        if (menuService.selectCountRoleMenuByMenuId(menuId) > 0)
        {
            return AjaxResult.warn("菜单已分配,不允许删除");
        }
        AuthorizationUtils.clearAllCachedAuthorizationInfo();
        return toAjax(menuService.deleteMenuById(menuId));
    }

    /**
     * 新增
     */
    @GetMapping("/add/{parentId}")
    public String add(@PathVariable("parentId") Long parentId, ModelMap mmap)
    {
        Menu menu = null;
        if (0L != parentId)
        {
            menu = menuService.selectMenuById(parentId);
        }
        else
        {
            menu = new Menu();
            menu.setMenuId(0L);
            menu.setMenuName("主目录");
        }
        mmap.put("menu", menu);
        return prefix + "/add";
    }

    /**
     * 新增保存菜单
     */
    @Log(title = "菜单管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("system:menu:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(@Validated Menu menu)
    {
        if (UserConstants.MENU_NAME_NOT_UNIQUE.equals(menuService.checkMenuNameUnique(menu)))
        {
            return error("新增菜单'" + menu.getMenuName() + "'失败，菜单名称已存在");
        }
        AuthorizationUtils.clearAllCachedAuthorizationInfo();
        return toAjax(menuService.insertMenu(menu));
    }

    /**
     * 修改菜单
     */
    @RequiresPermissions("system:menu:edit")
    @GetMapping("/edit/{menuId}")
    public String edit(@PathVariable("menuId") Long menuId, ModelMap mmap)
    {
        mmap.put("menu", menuService.selectMenuById(menuId));
        return prefix + "/edit";
    }

    /**
     * 修改保存菜单
     */
    @Log(title = "菜单管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("system:menu:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(@Validated Menu menu)
    {
        if (UserConstants.MENU_NAME_NOT_UNIQUE.equals(menuService.checkMenuNameUnique(menu)))
        {
            return error("修改菜单'" + menu.getMenuName() + "'失败，菜单名称已存在");
        }
        AuthorizationUtils.clearAllCachedAuthorizationInfo();
        return toAjax(menuService.updateMenu(menu));
    }

    /**
     * 选择菜单图标
     */
    @GetMapping("/icon")
    public String icon()
    {
        return prefix + "/icon";
    }

    /**
     * 校验菜单名称
     */
    @PostMapping("/checkMenuNameUnique")
    @ResponseBody
    public String checkMenuNameUnique(Menu menu)
    {
        return menuService.checkMenuNameUnique(menu);
    }

    /**
     * 加载角色菜单列表树
     */
    @GetMapping("/roleMenuTreeData")
    @ResponseBody
    public List<Ztree> roleMenuTreeData(Role role)
    {
        List<Ztree> ztrees = menuService.roleMenuTreeData(role);
        return ztrees;
    }

    /**
     * 加载所有菜单列表树
     */
    @GetMapping("/menuTreeData")
    @ResponseBody
    public List<Ztree> menuTreeData(Role role)
    {
        List<Ztree> ztrees = menuService.menuTreeData();
        return ztrees;
    }

    /**
     * 选择菜单树
     */
    @GetMapping("/selectMenuTree/{menuId}")
    public String selectMenuTree(@PathVariable("menuId") Long menuId, ModelMap mmap)
    {
        mmap.put("menu", menuService.selectMenuById(menuId));
        return prefix + "/tree";
    }
}