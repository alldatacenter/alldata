package com.vince.xq.project.system.user.controller;

import com.vince.xq.common.constant.UserConstants;
import com.vince.xq.common.utils.DateUtils;
import com.vince.xq.common.utils.StringUtils;
import com.vince.xq.common.utils.file.FileUploadUtils;
import com.vince.xq.common.utils.file.MimeTypeUtils;
import com.vince.xq.framework.aspectj.lang.annotation.Log;
import com.vince.xq.framework.aspectj.lang.enums.BusinessType;
import com.vince.xq.framework.config.RuoYiConfig;
import com.vince.xq.framework.shiro.service.PasswordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import com.vince.xq.framework.web.controller.BaseController;
import com.vince.xq.framework.web.domain.AjaxResult;
import com.vince.xq.project.system.user.domain.User;
import com.vince.xq.project.system.user.service.IUserService;

/**
 * 个人信息 业务处理
 * 
 * @author ruoyi
 */
@Controller
@RequestMapping("/system/user/profile")
public class ProfileController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    private String prefix = "system/user/profile";

    @Autowired
    private IUserService userService;

    @Autowired
    private PasswordService passwordService;

    /**
     * 个人信息
     */
    @GetMapping()
    public String profile(ModelMap mmap)
    {
        User user = getSysUser();
        mmap.put("user", user);
        mmap.put("roleGroup", userService.selectUserRoleGroup(user.getUserId()));
        mmap.put("postGroup", userService.selectUserPostGroup(user.getUserId()));
        return prefix + "/profile";
    }

    @GetMapping("/checkPassword")
    @ResponseBody
    public boolean checkPassword(String password)
    {
        User user = getSysUser();
        if (passwordService.matches(user, password))
        {
            return true;
        }
        return false;
    }

    @GetMapping("/resetPwd")
    public String resetPwd(ModelMap mmap)
    {
        User user = getSysUser();
        mmap.put("user", userService.selectUserById(user.getUserId()));
        return prefix + "/resetPwd";
    }

    @Log(title = "重置密码", businessType = BusinessType.UPDATE)
    @PostMapping("/resetPwd")
    @ResponseBody
    public AjaxResult resetPwd(String oldPassword, String newPassword)
    {
        User user = getSysUser();
        if (!passwordService.matches(user, oldPassword))
        {
            return error("修改密码失败，旧密码错误");
        }
        if (passwordService.matches(user, newPassword))
        {
            return error("新密码不能与旧密码相同");
        }
        user.setPassword(newPassword);
        user.setPwdUpdateDate(DateUtils.getNowDate());
        if (userService.resetUserPwd(user) > 0)
        {
            setSysUser(userService.selectUserById(user.getUserId()));
            return success();
        }
        return error("修改密码异常，请联系管理员");
    }

    /**
     * 修改用户
     */
    @GetMapping("/edit")
    public String edit(ModelMap mmap)
    {
        User user = getSysUser();
        mmap.put("user", userService.selectUserById(user.getUserId()));
        return prefix + "/edit";
    }

    /**
     * 修改头像
     */
    @GetMapping("/avatar")
    public String avatar(ModelMap mmap)
    {
        User user = getSysUser();
        mmap.put("user", userService.selectUserById(user.getUserId()));
        return prefix + "/avatar";
    }

    /**
     * 修改用户
     */
    @Log(title = "个人信息", businessType = BusinessType.UPDATE)
    @PostMapping("/update")
    @ResponseBody
    public AjaxResult update(User user)
    {
        User currentUser = getSysUser();
        currentUser.setUserName(user.getUserName());
        currentUser.setEmail(user.getEmail());
        currentUser.setPhonenumber(user.getPhonenumber());
        currentUser.setSex(user.getSex());
        if (StringUtils.isNotEmpty(user.getPhonenumber())
                && UserConstants.USER_PHONE_NOT_UNIQUE.equals(userService.checkPhoneUnique(currentUser)))
        {
            return error("修改用户'" + currentUser.getLoginName() + "'失败，手机号码已存在");
        }
        else if (StringUtils.isNotEmpty(user.getEmail())
                && UserConstants.USER_EMAIL_NOT_UNIQUE.equals(userService.checkEmailUnique(currentUser)))
        {
            return error("修改用户'" + currentUser.getLoginName() + "'失败，邮箱账号已存在");
        }
        if (userService.updateUserInfo(currentUser) > 0)
        {
            setSysUser(userService.selectUserById(currentUser.getUserId()));
            return success();
        }
        return error();
    }

    /**
     * 保存头像
     */
    @Log(title = "个人信息", businessType = BusinessType.UPDATE)
    @PostMapping("/updateAvatar")
    @ResponseBody
    public AjaxResult updateAvatar(@RequestParam("avatarfile") MultipartFile file)
    {
        User currentUser = getSysUser();
        try
        {
            if (!file.isEmpty())
            {
                String avatar = FileUploadUtils.upload(RuoYiConfig.getAvatarPath(), file, MimeTypeUtils.IMAGE_EXTENSION);
                currentUser.setAvatar(avatar);
                if (userService.updateUserInfo(currentUser) > 0)
                {
                    setSysUser(userService.selectUserById(currentUser.getUserId()));
                    return success();
                }
            }
            return error();
        }
        catch (Exception e)
        {
            log.error("修改头像失败！", e);
            return error(e.getMessage());
        }
    }
}
