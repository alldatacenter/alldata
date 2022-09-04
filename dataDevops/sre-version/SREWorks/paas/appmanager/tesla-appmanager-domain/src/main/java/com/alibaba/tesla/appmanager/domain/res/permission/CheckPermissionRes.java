package com.alibaba.tesla.appmanager.domain.res.permission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 检查权限返回结果
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckPermissionRes {

    /**
     * 经检查后认定持有的权限点列表
     */
    private List<String> permissions = new ArrayList<>();
}
