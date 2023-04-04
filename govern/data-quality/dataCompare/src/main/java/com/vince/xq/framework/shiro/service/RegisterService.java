package com.vince.xq.framework.shiro.service;

import com.vince.xq.common.constant.Constants;
import com.vince.xq.common.constant.ShiroConstants;
import com.vince.xq.common.constant.UserConstants;
import com.vince.xq.common.utils.ServletUtils;
import com.vince.xq.framework.manager.AsyncManager;
import com.vince.xq.framework.manager.factory.AsyncFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.vince.xq.common.utils.DateUtils;
import com.vince.xq.common.utils.MessageUtils;
import com.vince.xq.common.utils.StringUtils;
import com.vince.xq.project.system.user.domain.User;
import com.vince.xq.project.system.user.service.IUserService;

/**
 * 注册校验方法
 * 
 * @author ruoyi
 */
@Component
public class RegisterService
{
    @Autowired
    private IUserService userService;

    /**
     * 注册
     */
    public String register(User user)
    {
        String msg = "", loginName = user.getLoginName(), password = user.getPassword();

        if (ShiroConstants.CAPTCHA_ERROR.equals(ServletUtils.getRequest().getAttribute(ShiroConstants.CURRENT_CAPTCHA)))
        {
            msg = "验证码错误";
        }
        else if (StringUtils.isEmpty(loginName))
        {
            msg = "用户名不能为空";
        }
        else if (StringUtils.isEmpty(password))
        {
            msg = "用户密码不能为空";
        }
        else if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
        {
            msg = "密码长度必须在5到20个字符之间";
        }
        else if (loginName.length() < UserConstants.USERNAME_MIN_LENGTH
                || loginName.length() > UserConstants.USERNAME_MAX_LENGTH)
        {
            msg = "账户长度必须在2到20个字符之间";
        }
        else if (UserConstants.USER_NAME_NOT_UNIQUE.equals(userService.checkLoginNameUnique(user)))
        {
            msg = "保存用户'" + loginName + "'失败，注册账号已存在";
        }
        else
        {
            user.setPwdUpdateDate(DateUtils.getNowDate());
            user.setUserName(loginName);
            boolean regFlag = userService.registerUser(user);
            if (!regFlag)
            {
                msg = "注册失败,请联系系统管理人员";
            }
            else
            {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(loginName, Constants.REGISTER, MessageUtils.message("user.register.success")));
            }
        }
        return msg;
    }
}
