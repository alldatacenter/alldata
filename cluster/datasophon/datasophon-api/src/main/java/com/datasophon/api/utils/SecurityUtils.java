package com.datasophon.api.utils;


import cn.hutool.core.convert.Convert;
import com.datasophon.common.Constants;
import com.datasophon.dao.entity.UserInfoEntity;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;


public class SecurityUtils {

    public static HttpServletRequest getRequest() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        return request;
    }

    public static HttpSession getSession() {
        HttpSession session = getRequest().getSession();
        return session;
    }
    /**
     * 获取用户
     */
    public static String getUsername() {
        String username = getAuthUser().getUsername();
        return null == username ? null : ServletUtils.urlDecode(username);
    }
    /**
     * 获取用户ID
     */
    public static Long getUserId() {
        return Convert.toLong(ServletUtils.getRequest().getHeader(Constants.DETAILS_USER_ID));
    }


    /**
     * 是否为管理员
     *
     * @param userInfoEntity 用户
     * @return 结果
     */
    public static boolean isAdmin(UserInfoEntity userInfoEntity) {
        Integer userId = userInfoEntity.getId();
        return userId != null && 1 == userId;
    }


    public static UserInfoEntity getAuthUser() {
        return (UserInfoEntity) getRequest().getAttribute(Constants.SESSION_USER);
    }
}
