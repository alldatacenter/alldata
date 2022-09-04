package com.alibaba.tesla.authproxy.model.vo;

import com.alibaba.tesla.authproxy.Constants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 导入权限列表 VO
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportPermissionsVO {

    /**
     * 角色列表
     */
    private List<Role> roles;

    /**
     * OAM 中的角色前缀
     */
    private String prefix = Constants.PERMISSION_PREFIX_API;

    /**
     * 是否忽略 permissions 导入
     */
    private Boolean ignorePermissions = false;

    /**
     * 权限详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Permission {

        /**
         * 权限路径
         */
        private String path;
    }

    /**
     * 角色详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Role {

        /**
         * 角色名称
         */
        private String roleName;

        /**
         * 描述
         */
        private String description;

        /**
         * 角色拥有权限列表
         */
        private List<Permission> permissions;
    }
}
