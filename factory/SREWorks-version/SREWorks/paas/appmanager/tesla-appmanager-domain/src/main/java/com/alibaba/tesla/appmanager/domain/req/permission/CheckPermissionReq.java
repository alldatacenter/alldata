package com.alibaba.tesla.appmanager.domain.req.permission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限检查请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckPermissionReq {

    /**
     * 操作者标识
     */
    private String operator;

    /**
     * 待检查的权限点列表
     */
    private List<String> checkPermissions = new ArrayList<>();

    /**
     * 默认持有的权限点列表
     */
    private List<String> defaultPermissions = new ArrayList<>();

    /**
     * 扮演角色
     */
    private String asRole;
}
