package com.alibaba.tesla.authproxy.web;

import com.alibaba.tesla.authproxy.ApplicationException;
import com.alibaba.tesla.authproxy.BaseController;
import com.alibaba.tesla.authproxy.lib.exceptions.PrivateAuthNotLogin;
import com.alibaba.tesla.authproxy.model.vo.AppMenuVO;
import com.alibaba.tesla.authproxy.model.vo.MenuDoTreeVO;
import com.alibaba.tesla.authproxy.service.AuthServiceManager;
import com.alibaba.tesla.common.base.TeslaResult;
import com.alibaba.tesla.common.utils.TeslaResultBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * <p>Title: MenuController.java<／p>
 * <p>Description: <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
@Controller
@RequestMapping("auth/menu")
public class MenuController extends BaseController {

    /**
     * 初始化某个应用下的菜单，并全部授权给该应用下的默认角色
     *
     * @param request
     * @return
     */
    @RequestMapping(value = "/init", method = RequestMethod.POST)
    @ResponseBody
    public com.alibaba.tesla.common.base.TeslaResult initNew(HttpServletRequest request, @RequestBody AppMenuVO appMenu)
            throws ApplicationException, PrivateAuthNotLogin {
        if (null == appMenu || null == appMenu.getMenus() || appMenu.getMenus().size() == 0) {
            return TeslaResultBuilder.errorResult(null, "初始化数据为空");
        }
        if (null == appMenu.getAppId() || appMenu.getAppId().length() == 0) {
            return TeslaResultBuilder.errorResult(null, "应用ID不能为空");
        }
        authPolicy.getAuthServiceManager().initMenuByAppNew(this.getLoginUser(request), appMenu);
        return TeslaResultBuilder.successResult();
    }

    /**
     * 查询菜单
     *
     * @param request
     * @param appId   应用ID，如ads传入tesla_ads
     * @return 返回菜单树形结构数据
     */
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> listMenu(HttpServletRequest request, @RequestParam String appId)
            throws PrivateAuthNotLogin {
        if (null == appId || appId.length() == 0) {
            return buildErrorResult("应用ID不能为空");
        }
        AuthServiceManager authManager = this.authPolicy.getAuthServiceManager();
        List<MenuDoTreeVO> menuPermissionData = authManager.listMenuByRole(getLoginUser(request), appId);
        return buildResult(menuPermissionData);
    }

    @RequestMapping(value = "/all", method = RequestMethod.GET)
    @ResponseBody
    public com.alibaba.tesla.common.base.TeslaResult listAll(HttpServletRequest request, @RequestParam String appId) throws PrivateAuthNotLogin {
        if (null == appId || appId.length() == 0) {
            return TeslaResultBuilder.errorResult(null, "应用ID不能为空");
        }
        AuthServiceManager authManager = this.authPolicy.getAuthServiceManager();
        List<MenuDoTreeVO> menuPermissionData = authManager.listMenuByApp(this.getLoginUser(request), appId);
        return TeslaResultBuilder.successResult(menuPermissionData);
    }

    /**
     * 批量添加菜单
     *
     * @param request
     * @return
     */
    @RequestMapping(value = "/batchAdd", method = RequestMethod.POST)
    @ResponseBody
    @Deprecated
    public TeslaResult batchAddMenu(HttpServletRequest request, @RequestBody AppMenuVO appMenu)
            throws ApplicationException, PrivateAuthNotLogin {
        int count = authPolicy.getAuthServiceManager().batchAddMenu(this.getLoginUser(request), appMenu);
        return TeslaResultBuilder.successResult(count);
    }
}