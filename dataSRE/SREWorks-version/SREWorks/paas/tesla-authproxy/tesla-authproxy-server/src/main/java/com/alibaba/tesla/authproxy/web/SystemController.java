package com.alibaba.tesla.authproxy.web;

import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.model.RoleDO;
import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.model.mapper.RoleMapper;
import com.alibaba.tesla.authproxy.model.mapper.RolePermissionRelMapper;
import com.alibaba.tesla.authproxy.model.mapper.UserMapper;
import com.alibaba.tesla.authproxy.model.mapper.UserRoleRelMapper;
import com.alibaba.tesla.authproxy.service.AuthPolicy;
import com.alibaba.tesla.authproxy.service.TeslaUserService;
import com.alibaba.tesla.authproxy.service.job.elasticjob.SyncOamRoleJob;
import com.alibaba.tesla.authproxy.util.UserUtil;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.common.base.TeslaResultFactory;
import com.alibaba.tesla.web.controller.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统维护 Controller
 */
@RestController
@Slf4j
public class SystemController extends BaseController {

    @Autowired
    private TeslaUserService teslaUserService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private RolePermissionRelMapper rolePermissionRelMapper;

    @Autowired
    private UserRoleRelMapper userRoleRelMapper;

    @Autowired
    private AuthPolicy authPolicy;

//    @Autowired
//    private SyncOamRoleJob syncOamRoleJob;

    /**
     * 选择 5 用户填充 tenantId 和 userId 字段
     */
    @RequestMapping(value = "system/fillUserId", method = RequestMethod.GET)
    @ResponseBody
    public TeslaBaseResult fillUserId() {
        List<UserDO> users = userMapper.selectNoUserId();
        for (UserDO user : users) {
            user.setTenantId(Constants.DEFAULT_TENANT_ID);
            user.setUserId(UserUtil.getUserId(user));
            teslaUserService.update(user);
        }
        return TeslaResultFactory.buildSucceedResult(users);
    }

    /**
     * 选择 5 用户填充 depId 字段
     */
    @RequestMapping(value = "system/fillDepId", method = RequestMethod.GET)
    @ResponseBody
    public TeslaBaseResult fillDepId(HttpServletRequest request) {
        String limit = request.getParameter("limit");
        String offset = request.getParameter("offset");
        if (StringUtils.isEmpty(offset) || StringUtils.isEmpty(limit)) {
            return TeslaResultFactory.buildClientErrorResult("offset/limit is required");
        }

        List<UserDO> users = userMapper.selectByPages(Integer.parseInt(limit), Integer.parseInt(offset));
        Map<String, String> result = new HashMap<>();
        for (UserDO user : users) {
            String empId = user.getEmpId();
            UserDO bucUser;
            try {
                bucUser = authPolicy.getAuthServiceManager().getUserByEmpId(empId);
            } catch (Exception e) {
                result.put(empId, ExceptionUtils.getStackTrace(e));
                continue;
            }
            if (bucUser == null) {
                result.put(empId, "No buc user found");
                continue;
            }

            user.setDepId(bucUser.getDepId());
            teslaUserService.update(user);
            result.put(empId, bucUser.getDepId());
        }
        return TeslaResultFactory.buildSucceedResult(result);
    }

    /**
     * 清理权限数据
     */
    @GetMapping(value = "system/cleanPermissions")
    @ResponseBody
    public TeslaBaseResult cleanPermissions(HttpServletRequest request) {
        if ("tesla".equals(request.getParameter("code"))) {
            rolePermissionRelMapper.deleteAll();
            userRoleRelMapper.deleteAll();
            roleMapper.deleteAll();
            return TeslaResultFactory.buildSucceedResult();
        }
        return TeslaResultFactory.buildForbiddenResult();
    }

    /**
     * 清理权限数据
     */
    @GetMapping(value = "system/syncRoles")
    @ResponseBody
    public TeslaBaseResult syncOamRoles(HttpServletRequest request) {
//        String appId = request.getParameter("appId");
//        if (StringUtils.isEmpty(appId)) {
//            return TeslaResultFactory.buildForbiddenResult();
//        }
//
//        // 当 DB 中获取当前的所有角色
//        List<RoleDO> localRoles = roleMapper
//            .findAllByTenantIdAndLocale(Constants.DEFAULT_TENANT_ID, Constants.DEFAULT_LOCALE)
//            .stream()
//            .filter(p -> p.getRoleId().startsWith(appId + ":"))
//            .collect(Collectors.toList());
//        syncOamRoleJob.syncToOamRole(localRoles);
        return TeslaResultFactory.buildSucceedResult();
    }
}
