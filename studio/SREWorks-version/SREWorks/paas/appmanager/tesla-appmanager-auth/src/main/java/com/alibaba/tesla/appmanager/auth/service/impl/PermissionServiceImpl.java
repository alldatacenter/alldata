package com.alibaba.tesla.appmanager.auth.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.tesla.appmanager.auth.service.PermissionService;
import com.alibaba.tesla.appmanager.auth.service.checker.PermissionChecker;
import com.alibaba.tesla.appmanager.domain.req.permission.CheckPermissionReq;
import com.alibaba.tesla.appmanager.domain.res.permission.CheckPermissionRes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Permission 服务，用于校验权限
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class PermissionServiceImpl implements PermissionService {

    @Autowired
    private PermissionChecker permissionChecker;

    /**
     * 权限点检查
     *
     * @param request 检查请求
     * @return 持有的权限点列表
     */
    @Override
    public CheckPermissionRes checkPermission(CheckPermissionReq request) {
        CheckPermissionRes result = permissionChecker.checkPermissions(request);
        log.info("action=checkPermission|operator={}|checkPermissions={}|result={}", request.getOperator(),
                JSONArray.toJSONString(request.getCheckPermissions()),
                JSONArray.toJSONString(result.getPermissions()));
        return result;
    }
}
