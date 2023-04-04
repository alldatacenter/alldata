package com.vince.xq.project.system.user.controller;

import java.util.Date;
import java.util.List;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import com.vince.xq.common.constant.ShiroConstants;
import com.vince.xq.common.utils.CookieUtils;
import com.vince.xq.common.utils.DateUtils;
import com.vince.xq.common.utils.ServletUtils;
import com.vince.xq.common.utils.StringUtils;
import com.vince.xq.common.utils.text.Convert;
import com.vince.xq.framework.config.RuoYiConfig;
import com.vince.xq.framework.shiro.service.PasswordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.vince.xq.framework.web.controller.BaseController;
import com.vince.xq.framework.web.domain.AjaxResult;
import com.vince.xq.project.system.config.service.IConfigService;
import com.vince.xq.project.system.menu.domain.Menu;
import com.vince.xq.project.system.menu.service.IMenuService;
import com.vince.xq.project.system.user.domain.User;

/**
 * 首页 业务处理
 * 
 * @author ruoyi
 */
@Controller
public class IndexController extends BaseController
{
    @Autowired
    private IMenuService menuService;

    @Autowired
    private IConfigService configService;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private RuoYiConfig ruoYiConfig;

    // 系统首页
    @GetMapping("/index")
    public String index(ModelMap mmap)
    {
        // 取身份信息
        User user = getSysUser();
        // 根据用户id取出菜单
        List<Menu> menus = menuService.selectMenusByUser(user);
        mmap.put("menus", menus);
        mmap.put("user", user);
        mmap.put("sideTheme", configService.selectConfigByKey("sys.index.sideTheme"));
        mmap.put("skinName", configService.selectConfigByKey("sys.index.skinName"));
        Boolean footer = Convert.toBool(configService.selectConfigByKey("sys.index.footer"), true);
        Boolean tagsView = Convert.toBool(configService.selectConfigByKey("sys.index.tagsView"), true);
        mmap.put("footer", footer);
        mmap.put("tagsView", tagsView);
        mmap.put("mainClass", contentMainClass(footer, tagsView));
        mmap.put("copyrightYear", ruoYiConfig.getCopyrightYear());
        mmap.put("demoEnabled", ruoYiConfig.isDemoEnabled());
        mmap.put("isDefaultModifyPwd", initPasswordIsModify(user.getPwdUpdateDate()));
        mmap.put("isPasswordExpired", passwordIsExpiration(user.getPwdUpdateDate()));
        mmap.put("isMobile", ServletUtils.checkAgentIsMobile(ServletUtils.getRequest().getHeader("User-Agent")));

        // 菜单导航显示风格
        String menuStyle = configService.selectConfigByKey("sys.index.menuStyle");
        // 移动端，默认使左侧导航菜单，否则取默认配置
        String indexStyle = ServletUtils.checkAgentIsMobile(ServletUtils.getRequest().getHeader("User-Agent")) ? "index" : menuStyle;
        
        // 优先Cookie配置导航菜单
        Cookie[] cookies = ServletUtils.getRequest().getCookies();
        for (Cookie cookie : cookies)
        {
            if (StringUtils.isNotEmpty(cookie.getName()) && "nav-style".equalsIgnoreCase(cookie.getName()))
            {
                indexStyle = cookie.getValue();
                break;
            }
        }
        String webIndex = "topnav".equalsIgnoreCase(indexStyle) ? "index-topnav" : "index";
        return webIndex;
    }

    // 锁定屏幕
    @GetMapping("/lockscreen")
    public String lockscreen(ModelMap mmap)
    {
        mmap.put("user", getSysUser());
        ServletUtils.getSession().setAttribute(ShiroConstants.LOCK_SCREEN, true);
        return "lock";
    }

    // 解锁屏幕
    @PostMapping("/unlockscreen")
    @ResponseBody
    public AjaxResult unlockscreen(String password)
    {
        User user = getSysUser();
        if (StringUtils.isNull(user))
        {
            return AjaxResult.error("服务器超时，请重新登录");
        }
        if (passwordService.matches(user, password))
        {
            ServletUtils.getSession().removeAttribute(ShiroConstants.LOCK_SCREEN);
            return AjaxResult.success();
        }
        return AjaxResult.error("密码不正确，请重新输入。");
    }

    // 切换主题
    @GetMapping("/system/switchSkin")
    public String switchSkin()
    {
        return "skin";
    }

    // 切换菜单
    @GetMapping("/system/menuStyle/{style}")
    public void menuStyle(@PathVariable String style, HttpServletResponse response)
    {
        CookieUtils.setCookie(response, "nav-style", style);
    }

    // 系统介绍
    @GetMapping("/system/main")
    public String main(ModelMap mmap)
    {
        mmap.put("version", ruoYiConfig.getVersion());
        return "main";
    }

    // content-main class
    public String contentMainClass(Boolean footer, Boolean tagsView)
    {
        if (!footer && !tagsView)
        {
            return "tagsview-footer-hide";
        }
        else if (!footer)
        {
            return "footer-hide";
        }
        else if (!tagsView)
        {
            return "tagsview-hide";
        }
        return StringUtils.EMPTY;
    }

    // 检查初始密码是否提醒修改
    public boolean initPasswordIsModify(Date pwdUpdateDate)
    {
        Integer initPasswordModify = Convert.toInt(configService.selectConfigByKey("sys.account.initPasswordModify"));
        return initPasswordModify != null && initPasswordModify == 1 && pwdUpdateDate == null;
    }
    
    // 检查密码是否过期
    public boolean passwordIsExpiration(Date pwdUpdateDate)
    {
        Integer passwordValidateDays = Convert.toInt(configService.selectConfigByKey("sys.account.passwordValidateDays"));
        if (passwordValidateDays !=null && passwordValidateDays > 0)
        {
            if (StringUtils.isNull(pwdUpdateDate))
            {
                // 如果从未修改过初始密码，直接提醒过期
                return true;
            }
            Date nowDate = DateUtils.getNowDate();
            return DateUtils.differentDaysByMillisecond(nowDate, pwdUpdateDate) > passwordValidateDays;
        }
        return false;
    }
}
