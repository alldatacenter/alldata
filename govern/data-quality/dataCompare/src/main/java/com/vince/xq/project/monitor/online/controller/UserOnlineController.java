package com.vince.xq.project.monitor.online.controller;

import java.util.List;

import com.vince.xq.common.utils.security.ShiroUtils;
import com.vince.xq.common.utils.text.Convert;
import com.vince.xq.framework.aspectj.lang.annotation.Log;
import com.vince.xq.framework.aspectj.lang.enums.BusinessType;
import com.vince.xq.framework.shiro.session.OnlineSessionDAO;
import com.vince.xq.framework.web.controller.BaseController;
import com.vince.xq.framework.web.domain.AjaxResult;
import com.vince.xq.framework.web.page.TableDataInfo;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.vince.xq.project.monitor.online.domain.OnlineSession;
import com.vince.xq.project.monitor.online.domain.OnlineSession.OnlineStatus;
import com.vince.xq.project.monitor.online.domain.UserOnline;
import com.vince.xq.project.monitor.online.service.IUserOnlineService;

/**
 * 在线用户监控
 * 
 * @author ruoyi
 */
@Controller
@RequestMapping("/monitor/online")
public class UserOnlineController extends BaseController
{
    private String prefix = "monitor/online";

    @Autowired
    private IUserOnlineService userOnlineService;

    @Autowired
    private OnlineSessionDAO onlineSessionDAO;

    @RequiresPermissions("monitor:online:view")
    @GetMapping()
    public String online()
    {
        return prefix + "/online";
    }

    @RequiresPermissions("monitor:online:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(UserOnline userOnline)
    {
        startPage();
        List<UserOnline> list = userOnlineService.selectUserOnlineList(userOnline);
        return getDataTable(list);
    }

    @RequiresPermissions(value = { "monitor:online:batchForceLogout", "monitor:online:forceLogout" }, logical = Logical.OR)
    @Log(title = "在线用户", businessType = BusinessType.FORCE)
    @PostMapping("/batchForceLogout")
    @ResponseBody
    public AjaxResult batchForceLogout(String ids)
    {
        for (String sessionId : Convert.toStrArray(ids))
        {
            UserOnline online = userOnlineService.selectOnlineById(sessionId);
            if (online == null)
            {
                return error("用户已下线");
            }
            OnlineSession onlineSession = (OnlineSession) onlineSessionDAO.readSession(online.getSessionId());
            if (onlineSession == null)
            {
                return error("用户已下线");
            }
            if (sessionId.equals(ShiroUtils.getSessionId()))
            {
                return error("当前登录用户无法强退");
            }
            onlineSessionDAO.delete(onlineSession);
            online.setStatus(OnlineStatus.off_line);
            userOnlineService.saveOnline(online);
            userOnlineService.removeUserCache(online.getLoginName(), sessionId);
        }
        return success();
    }
}
