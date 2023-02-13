package com.platform.utils;


import cn.datax.service.system.api.dto.JwtUserDto;
import cn.datax.service.system.api.feign.UserServiceFeign;
import com.platform.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@Component
public class SecurityUtil {

    @Autowired
    private UserServiceFeign userServiceFeign;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 获取用户
     *
     * @return user
     */
    public static JwtUserDto getDataUser() {
        UserServiceFeign userServiceFeign = SpringContextHolder.getBean(UserServiceFeign.class);
        return userServiceFeign.loginByUsername(getCurrentUsername());
    }

    public static String getCurrentUsername() {
        HttpServletRequest request =((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String authorization = request.getHeader("Authorization");
        String tokenSubjectObject = JwtUtil.getTokenSubjectObject(authorization);
        if (tokenSubjectObject == null) {
            throw new BadRequestException(HttpStatus.UNAUTHORIZED, "当前登录状态过期");
        }
        return tokenSubjectObject;

    }


    /**
     * 获取用户ID
     *
     * @return id
     */
    public static String getUserId() {
        JwtUserDto user = getDataUser();
        if (user != null){
            return user.getUser().getId()+"";
        }
        return "";
    }

    /**
     * 获取用户部门
     *
     * @return id
     */
    public static String getUserDeptId() {
        JwtUserDto user = getDataUser();
        if (user != null){
            return user.getUser().getDeptId()+"";
        }
        return "";
    }

    /**
     * 获取用户名称
     *
     * @return username
     */
    public static String getUserName() {
        JwtUserDto user = getDataUser();
        if (user != null){
            return user.getUsername();
        }
        return "";
    }

    /**
     * 获取用户昵称
     *
     * @return nickname
     */
    public static String getNickname() {
        JwtUserDto user = getDataUser();
        if (user != null){
            return user.getUser().getNickName();
        }
        return "";
    }

    /**
     * 获取用户角色
     *
     * @return username
     */
    public static List<String> getUserRoleIds() {
        JwtUserDto user = getDataUser();
        if (user != null){
            List<String> roles = new ArrayList<>(user.getRoles());
            return roles;
        }
        return null;
    }

    /**
     * 获取用户
     *
     * @return user
     */
    public static boolean isAdmin() {
        JwtUserDto user = getDataUser();
        if (user != null){
            return user.getUser().getIsAdmin();
        }
        return false;
    }
}
