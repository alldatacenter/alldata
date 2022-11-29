package cn.datax.common.utils;

import cn.datax.common.core.DataRole;
import cn.datax.common.core.DataUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.stream.Collectors;

public class SecurityUtil {

    /**
     * 获取用户
     *
     * @return user
     */
    public static DataUser getDataUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof DataUser) {
                DataUser user = (DataUser) principal;
                return user;
            }
        }
        return null;
    }

    /**
     * 获取用户ID
     *
     * @return id
     */
    public static String getUserId() {
        DataUser user = getDataUser();
        if (user != null){
            return user.getId();
        }
        return "";
    }

    /**
     * 获取用户部门
     *
     * @return id
     */
    public static String getUserDeptId() {
        DataUser user = getDataUser();
        if (user != null){
            return user.getDept();
        }
        return "";
    }

    /**
     * 获取用户名称
     *
     * @return username
     */
    public static String getUserName() {
        DataUser user = getDataUser();
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
        DataUser user = getDataUser();
        if (user != null){
            return user.getNickname();
        }
        return "";
    }

    /**
     * 获取用户角色
     *
     * @return username
     */
    public static List<String> getUserRoleIds() {
        DataUser user = getDataUser();
        if (user != null){
            List<String> roles = user.getRoles().stream().map(DataRole::getId).collect(Collectors.toList());
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
        DataUser user = getDataUser();
        if (user != null){
            return user.isAdmin();
        }
        return false;
    }
}
