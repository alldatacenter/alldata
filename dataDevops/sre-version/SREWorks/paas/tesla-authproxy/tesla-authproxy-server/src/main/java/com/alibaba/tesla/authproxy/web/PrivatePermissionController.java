package com.alibaba.tesla.authproxy.web;

import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.lib.exceptions.*;
import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.service.PrivateAuthService;
import com.alibaba.tesla.authproxy.service.PrivatePermissionService;
import com.alibaba.tesla.authproxy.service.UserRoleService;
import com.alibaba.tesla.authproxy.util.audit.*;
import com.alibaba.tesla.authproxy.web.common.PrivateBaseController;
import com.alibaba.tesla.authproxy.web.common.PrivateResultBuilder;
import com.alibaba.tesla.authproxy.web.input.PrivatePermissionRoleAccountAddParam;
import com.alibaba.tesla.authproxy.web.input.PrivatePermissionRoleAccountDeleteParam;
import com.alibaba.tesla.authproxy.web.input.PrivatePermissionRoleAccountParam;
import com.alibaba.tesla.authproxy.web.output.PrivatePermissionRoleAccountResult;
import com.alibaba.tesla.authproxy.web.output.PrivatePermissionRoleResult;
import com.alibaba.tesla.common.utils.TeslaResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

/**
 * 专有云 - 权限 API
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@RestController
@RequestMapping("auth/private/permission")
@Slf4j
public class PrivatePermissionController extends PrivateBaseController {

    @Autowired
    private PrivatePermissionService permissionService;

    @Autowired
    private PrivateAuthService authService;

    @Autowired
    private AuditUtil auditUtil;

    @Autowired
    private UserRoleService userRoleService;

    private Gson gson = new GsonBuilder().serializeNulls().create();

    /**
     * 角色列表获取 API
     */
    @RequestMapping(value = "role", method = RequestMethod.GET)
    @ResponseBody
    public TeslaResult role(HttpServletRequest request, HttpServletResponse response)
            throws PrivateAuthNotLogin, PrivateAuthForbidden, AuthProxyThirdPartyError, PrivateInternalError {
        // 检验是否拥有云账号管理权限
        UserDO userDo = (UserDO) request.getAttribute(Constants.REQUEST_ATTR_USER);
        authService.interceptFirstLogin(userDo);
        authService.checkAccountManagerPermission(userDo);

        // 校验通过，获取当前的角色列表
        PrivatePermissionRoleResult result = permissionService.getRoles(userDo);
        auditUtil.info(userDo, AuditTargetEnum.DATA, AuditActionEnum.SELECT, AuditOutcomeEnum.SUCCESS,
                String.format("authproxy - get permission roles, result=%s", gson.toJson(result)),
                AuditReasonEnum.AUTHORIZED);
        return PrivateResultBuilder.buildExtSuccessResult(result);
    }

    @RequestMapping(value = "role/account", method = RequestMethod.GET)
    @ResponseBody
    public TeslaResult roleAccount(@Valid @ModelAttribute PrivatePermissionRoleAccountParam param,
                                   BindingResult bindingResult, HttpServletRequest request)
            throws PrivateAuthNotLogin, PrivateAuthForbidden, AuthProxyThirdPartyError, PrivateValidationError,
            PrivateInternalError {
        UserDO userDo = (UserDO) request.getAttribute(Constants.REQUEST_ATTR_USER);
        if (bindingResult.hasErrors()) {
            auditUtil.info(userDo, AuditTargetEnum.DATA, AuditActionEnum.SELECT, AuditOutcomeEnum.FAILURE,
                    String.format("authproxy - get role accounts, validationError=%s",
                            gson.toJson(bindingResult.getFieldErrors())),
                    AuditReasonEnum.AUTHORIZED);
            return buildValidationResult(bindingResult);
        }
        param.cleanSelf();

        // 检验是否拥有云账号管理权限
        authService.interceptFirstLogin(userDo);
        authService.checkAccountManagerPermission(userDo);

        // 校验通过，获取当前角色可授权的云账号列表
        PrivatePermissionRoleAccountResult result = permissionService.getRoleAccounts(
                userDo, param.getRoleName(), param.getFilter());
        auditUtil.info(userDo, AuditTargetEnum.DATA, AuditActionEnum.SELECT, AuditOutcomeEnum.SUCCESS,
                String.format("authproxy - get role accounts, result=%s", gson.toJson(result)),
                AuditReasonEnum.AUTHORIZED);
        return PrivateResultBuilder.buildExtSuccessResult(result);
    }

    @RequestMapping(value = "role/account/add", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public TeslaResult grantRoleToAccount(@Valid @RequestBody PrivatePermissionRoleAccountAddParam param,
                                          BindingResult bindingResult, HttpServletRequest request)
            throws PrivateAuthNotLogin, PrivateAuthForbidden, AuthProxyThirdPartyError, PrivateValidationError,
            PrivateInternalError {
        UserDO userDo = (UserDO) request.getAttribute(Constants.REQUEST_ATTR_USER);
        if (bindingResult.hasErrors()) {
            auditUtil.info(userDo, AuditTargetEnum.DATA, AuditActionEnum.CREATE, AuditOutcomeEnum.FAILURE,
                    String.format("authproxy - add role account, validationError=%s",
                            gson.toJson(bindingResult.getFieldErrors())),
                    AuditReasonEnum.AUTHORIZED);
            return buildValidationResult(bindingResult);
        }
        param.cleanSelf();

        // 检验是否拥有云账号管理权限
        authService.interceptFirstLogin(userDo);
        authService.checkAccountManagerPermission(userDo);

        // 校验通过，授权
        permissionService.grantRoleToAccount(userDo, param);
        auditUtil.info(userDo, AuditTargetEnum.DATA, AuditActionEnum.CREATE, AuditOutcomeEnum.SUCCESS,
                String.format("authproxy - add role account, param=%s", gson.toJson(param)),
                AuditReasonEnum.AUTHORIZED);
        return PrivateResultBuilder.buildExtSuccessResult();
    }

    @RequestMapping(value = "role/account/delete", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public TeslaResult deleteRoleFromAccount(@Valid @RequestBody PrivatePermissionRoleAccountDeleteParam param,
                                             BindingResult bindingResult, HttpServletRequest request)
            throws PrivateAuthNotLogin, PrivateAuthForbidden, AuthProxyThirdPartyError, PrivateValidationError,
            PrivateInternalError {
        UserDO userDo = (UserDO) request.getAttribute(Constants.REQUEST_ATTR_USER);
        if (bindingResult.hasErrors()) {
            auditUtil.info(userDo, AuditTargetEnum.DATA, AuditActionEnum.DELETE, AuditOutcomeEnum.FAILURE,
                    String.format("authproxy - delete role account, validationError=%s",
                            gson.toJson(bindingResult.getFieldErrors())),
                    AuditReasonEnum.AUTHORIZED);
            return buildValidationResult(bindingResult);
        }
        param.cleanSelf();

        // 检验是否拥有云账号管理权限
        authService.interceptFirstLogin(userDo);
        authService.checkAccountManagerPermission(userDo);

        // 校验通过，删除授权
        permissionService.deleteRoleFromAccount(userDo, param);
        auditUtil.info(userDo, AuditTargetEnum.DATA, AuditActionEnum.DELETE, AuditOutcomeEnum.SUCCESS,
                String.format("authproxy - delete role account, param=%s", gson.toJson(param)),
                AuditReasonEnum.AUTHORIZED);
        return PrivateResultBuilder.buildExtSuccessResult();
    }

    /**
     * 角色列表获取 (API 专用) API
     */
    @RequestMapping(value = "api/role", method = RequestMethod.GET)
    @ResponseBody
    public TeslaResult apiRole(HttpServletRequest request, HttpServletResponse response)
        throws PrivateAuthNotLogin, PrivateAuthForbidden, AuthProxyThirdPartyError, PrivateInternalError {
        // 检验是否拥有云账号管理权限
        UserDO userDo = (UserDO) request.getAttribute(Constants.REQUEST_ATTR_USER);
        authService.interceptFirstLogin(userDo);
        authService.checkAccountManagerPermission(userDo);

        // 校验通过，获取当前的角色列表
        PrivatePermissionRoleResult result = permissionService.getApiRoles(userDo);
        auditUtil.info(userDo, AuditTargetEnum.DATA, AuditActionEnum.SELECT, AuditOutcomeEnum.SUCCESS,
            String.format("authproxy - get permission roles, result=%s", gson.toJson(result)),
            AuditReasonEnum.AUTHORIZED);
        return PrivateResultBuilder.buildExtSuccessResult(result);
    }
}
