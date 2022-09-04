package com.alibaba.tesla.authproxy.web;

import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.lib.exceptions.*;
import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.outbound.aas.AasClient;
import com.alibaba.tesla.authproxy.service.PrivateAccountService;
import com.alibaba.tesla.authproxy.service.PrivateAuthService;
import com.alibaba.tesla.authproxy.service.PrivatePermissionService;
import com.alibaba.tesla.authproxy.service.TeslaUserService;
import com.alibaba.tesla.authproxy.service.constant.UserTypeEnum;
import com.alibaba.tesla.authproxy.util.LocaleUtil;
import com.alibaba.tesla.authproxy.util.audit.*;
import com.alibaba.tesla.authproxy.web.common.PrivateBaseController;
import com.alibaba.tesla.authproxy.web.common.PrivateResultBuilder;
import com.alibaba.tesla.authproxy.web.input.*;
import com.alibaba.tesla.authproxy.web.output.*;
import com.alibaba.tesla.common.utils.TeslaResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

/**
 * 专有云 - 账号管理
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@RestController
@RequestMapping("auth/private/account")
@Slf4j
public class PrivateAccountController extends PrivateBaseController {

    @Autowired
    private LocaleUtil locale;

    @Autowired
    private PrivateAccountService accountService;

    @Autowired
    private PrivatePermissionService permissionService;

    @Autowired
    private PrivateAuthService authService;

    @Autowired
    private TeslaUserService teslaUserService;

    @Autowired
    private AuditUtil auditUtil;

    @Autowired
    private AasClient aasClient;

    private Gson gson = new GsonBuilder().serializeNulls().create();

    /**
     * 根据 aliyunId 获取 aliyunPk
     */
    @RequestMapping(value = "aliyunpk", method = RequestMethod.GET)
    @ResponseBody
    public TeslaResult getAliyunPkById(@Valid @ModelAttribute PrivateAccountAliyunPkByIdParam param,
                                       BindingResult bindingResult)
        throws PrivateValidationError {
        if (bindingResult.hasErrors()) {
            return buildValidationResult(bindingResult);
        }

        PrivateAccountAliyunResult result = accountService.getAliyunPkById(param.getAliyunId());
        return PrivateResultBuilder.buildSuccessResult(result);
    }

    /**
     * 根据 aliyunIds 获取 aliyunPk
     */
    @RequestMapping(value = "aliyunpks", method = RequestMethod.GET)
    @ResponseBody
    public TeslaResult getAliyunPksById(@Valid @ModelAttribute PrivateAccountAliyunPksByIdParam param,
                                       BindingResult bindingResult)
        throws PrivateValidationError {
        if (bindingResult.hasErrors()) {
            return buildValidationResult(bindingResult);
        }

        PrivateAccountAliyunListResult result = accountService.getAliyunPkByIds(param.getAliyunIds());
        return PrivateResultBuilder.buildSuccessResult(result);
    }

    /**
     * 根据 aliyunPk 获取 aliyunId
     */
    @RequestMapping(value = "aliyunid", method = RequestMethod.GET)
    @ResponseBody
    public TeslaResult getAliyunIdByPk(@Valid @ModelAttribute PrivateAccountAliyunIdByPkParam param,
                                       BindingResult bindingResult)
        throws PrivateValidationError {
        if (bindingResult.hasErrors()) {
            return buildValidationResult(bindingResult);
        }

        PrivateAccountAliyunResult result = accountService.getAliyunIdByPk(param.getAliyunPk());
        return PrivateResultBuilder.buildSuccessResult(result);
    }

    /**
     * 根据 aliyunPks 获取 aliyunIds
     */
    @RequestMapping(value = "aliyunids", method = RequestMethod.GET)
    @ResponseBody
    public TeslaResult getAliyunIdsByPk(@Valid @ModelAttribute PrivateAccountAliyunIdsByPkParam param,
                                        BindingResult bindingResult)
        throws PrivateValidationError {
        if (bindingResult.hasErrors()) {
            return buildValidationResult(bindingResult);
        }

        PrivateAccountAliyunListResult result = accountService.getAliyunIdByPks(param.getAliyunPks());
        return PrivateResultBuilder.buildSuccessResult(result);
    }

    ///**
    // * 根据 aliyunPk 获取用户的 Access Keys 信息
    // */
    //@RequestMapping(value = "accessKeys", method = RequestMethod.GET)
    //@ResponseBody
    //public TeslaResult getAccessKeysByPk(@Valid @ModelAttribute PrivateAccountAccessKeysByPkParam param,
    //                                     BindingResult bindingResult) throws AuthProxyThirdPartyError {
    //    if (bindingResult.hasErrors()) {
    //        return buildValidationResult(bindingResult);
    //    }
    //
    //    List<ListAccessKeysForAccountResponse.AccessKey> accessKeys = aasClient.getUserAccessKeys(param.getAliyunPk());
    //    return PrivateResultBuilder.buildSuccessResult(accessKeys);
    //}

    /**
     * 用户类型获取 API
     */
    @RequestMapping(value = "type", method = RequestMethod.GET)
    @ResponseBody
    public TeslaResult getUserType(HttpServletRequest request) throws PrivateInternalError, AuthProxyThirdPartyError {
        // 检查是否登录并获取当前用户
        UserDO userDo = (UserDO) request.getAttribute(Constants.REQUEST_ATTR_USER);

        // 校验通过，从 OAM 处获取对应的用户类型
        UserTypeEnum userType = permissionService.getUserType(userDo);
        PrivateAccountTypeResult result = new PrivateAccountTypeResult();
        result.setUserType(userType.toString());
        auditUtil.info(userDo, AuditTargetEnum.DATA, AuditActionEnum.SELECT, AuditOutcomeEnum.SUCCESS,
                String.format("authproxy - get user type, result=%s", gson.toJson(result)), AuditReasonEnum.AUTHORIZED);
        return PrivateResultBuilder.buildExtSuccessResult(result);
    }

    /**
     * 云账号获取列表 API
     */
    @RequestMapping(value = "", method = RequestMethod.GET)
    @ResponseBody
    public TeslaResult accountList(@Valid @ModelAttribute PrivateAccountListParam param,
                                   BindingResult bindingResult, HttpServletRequest request)
        throws AuthProxyThirdPartyError, PrivateInternalError {
        UserDO userDo = (UserDO) request.getAttribute(Constants.REQUEST_ATTR_USER);
        if (bindingResult.hasErrors()) {
            auditUtil.info(userDo, AuditTargetEnum.DATA, AuditActionEnum.SELECT, AuditOutcomeEnum.FAILURE,
                    String.format("authproxy - get account list, validationError=%s",
                            gson.toJson(bindingResult.getFieldErrors())), AuditReasonEnum.AUTHORIZED);
            return buildValidationResult(bindingResult);
        }
        param.cleanSelf();

        // 检查是否登录并获取当前用户
        UserTypeEnum userType = permissionService.getUserType(userDo);
        PrivateAccountListResult result;
        switch (userType) {
            case SUPERADMIN:
            case ACCOUNTADMIN:
                // 从数据库中获取对应的云账号的列表
                result = accountService.accountListBySearch(param.getSearch());
                break;
            default:
                // 获取自身作为云账号列表
                result = accountService.accountListByUser(userDo);
                break;
        }
        auditUtil.info(userDo, AuditTargetEnum.DATA, AuditActionEnum.SELECT, AuditOutcomeEnum.SUCCESS,
                String.format("authproxy - get account list, result=%s", gson.toJson(result)),
                AuditReasonEnum.AUTHORIZED);
        return PrivateResultBuilder.buildExtSuccessResult(result);
    }

    /**
     * 云账号获取新建 API
     */
    @RequestMapping(value = "add", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public TeslaResult accountAdd(@Valid @RequestBody PrivateAccountAddParam param,
                                  BindingResult bindingResult, HttpServletRequest request)
            throws PrivateAuthNotLogin, PrivateAuthForbidden, AuthProxyThirdPartyError, PrivateValidationError,
            PrivateInternalError {
        UserDO userDo = (UserDO) request.getAttribute(Constants.REQUEST_ATTR_USER);
        if (bindingResult.hasErrors()) {
            auditUtil.info(userDo, AuditTargetEnum.DATA, AuditActionEnum.CREATE, AuditOutcomeEnum.FAILURE,
                    String.format("authproxy - add account, validationError=%s",
                            gson.toJson(bindingResult.getFieldErrors())), AuditReasonEnum.AUTHORIZED);
            return buildValidationResult(bindingResult);
        }
        param.cleanSelf();

        // 检验是否拥有云账号管理权限（限制首次登录用户访问）
        authService.interceptFirstLogin(userDo);
        authService.checkAccountManagerPermission(userDo);

        // 校验通过，从数据库中获取对应的云账号的列表
        PrivateAccountAddResult result = accountService.accountAdd(param, request);
        auditUtil.info(userDo, AuditTargetEnum.DATA, AuditActionEnum.CREATE, AuditOutcomeEnum.SUCCESS,
                String.format("authproxy - add account, param=%s, result=%s", gson.toJson(param), gson.toJson(result)),
                AuditReasonEnum.AUTHORIZED);
        return PrivateResultBuilder.buildExtSuccessResult(result);
    }

    /**
     * 云账号密码修改 API
     */
    @RequestMapping(value = "password/change", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public TeslaResult accountPasswordChange(@Valid @RequestBody PrivateAccountPasswordChangeParam param,
                                             BindingResult bindingResult, HttpServletRequest request)
            throws PrivateAuthNotLogin, PrivateAuthForbidden, AuthProxyThirdPartyError, PrivateValidationError {
        UserDO userDo = (UserDO) request.getAttribute(Constants.REQUEST_ATTR_USER);
        if (bindingResult.hasErrors()) {
            auditUtil.info(userDo, AuditTargetEnum.DATA, AuditActionEnum.UPDATE, AuditOutcomeEnum.FAILURE,
                    String.format("authproxy - change password, validationError=%s",
                            gson.toJson(bindingResult.getFieldErrors())), AuditReasonEnum.AUTHORIZED);
            return buildValidationResult(bindingResult);
        }
        param.cleanSelf();

        // 检验是否拥有云账号管理权限（如果是自己修改自己的密码，不需要鉴权）
        if (!userDo.getLoginName().equals(param.getAliyunId())) {
            authService.interceptFirstLogin(userDo);
            authService.checkAccountManagerPermission(userDo);
        }

        // 检验是否为不可变用户
        UserDO targetUser = teslaUserService.getUserByLoginName(param.getAliyunId());
        if (null == targetUser) {
            throw new PrivateValidationError("aliyunId",
                    locale.msg("private.account.password.change.validation.aliyunId.notExist", param.getAliyunId()));
        }
        if (targetUser.getIsImmutable() > 0) {
            throw new PrivateValidationError("nonExistField",
                    locale.msg("private.account.password.change.validation.aliyunId.immutable", userDo.getLoginName()));
        }

        // 校验通过，进行密码修改
        accountService.passwordChange(param.getAliyunId(), param.getPassword());
        auditUtil.info(userDo, AuditTargetEnum.DATA, AuditActionEnum.UPDATE, AuditOutcomeEnum.SUCCESS,
                "authproxy - change password", AuditReasonEnum.AUTHORIZED);
        return PrivateResultBuilder.buildExtSuccessResult();
    }

    /**
     * 云账号信息修改 API
     */
    @RequestMapping(value = "info/change", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public TeslaResult accountInfoChange(@Valid @RequestBody PrivateAccountInfoChangeParam param,
                                         BindingResult bindingResult, HttpServletRequest request)
            throws PrivateAuthNotLogin, PrivateAuthForbidden, AuthProxyThirdPartyError, PrivateValidationError {
        UserDO userDo = (UserDO) request.getAttribute(Constants.REQUEST_ATTR_USER);
        if (bindingResult.hasErrors()) {
            auditUtil.info(userDo, AuditTargetEnum.DATA, AuditActionEnum.UPDATE, AuditOutcomeEnum.FAILURE,
                    String.format("authproxy - change info, validationError=%s",
                            gson.toJson(bindingResult.getFieldErrors())), AuditReasonEnum.AUTHORIZED);
            return buildValidationResult(bindingResult);
        }
        param.cleanSelf();

        // 检验是否拥有云账号管理权限（如果是自己修改自己的信息，不需要鉴权）
        if (!userDo.getLoginName().equals(param.getAliyunId())) {
            authService.interceptFirstLogin(userDo);
            authService.checkAccountManagerPermission(userDo);
        }

        // 校验通过，进行密码修改
        accountService.infoChange(param, request);
        auditUtil.info(userDo, AuditTargetEnum.DATA, AuditActionEnum.UPDATE, AuditOutcomeEnum.SUCCESS,
                String.format("authproxy - change info, param=%s", gson.toJson(param)), AuditReasonEnum.AUTHORIZED);
        return PrivateResultBuilder.buildExtSuccessResult();
    }

    /**
     * 账号锁定与解锁 API
     */
    @RequestMapping(value = "lock", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public TeslaResult lock(@Valid @RequestBody PrivateAccountLockParam param, BindingResult bindingResult,
                            HttpServletRequest request)
            throws PrivateValidationError, PrivateAuthForbidden, AuthProxyThirdPartyError, PrivateAuthNotLogin {
        UserDO userDo = (UserDO) request.getAttribute(Constants.REQUEST_ATTR_USER);
        if (bindingResult.hasErrors()) {
            auditUtil.info(userDo, AuditTargetEnum.DATA, AuditActionEnum.UPDATE, AuditOutcomeEnum.FAILURE,
                    String.format("authproxy - lock/unlock account, validationError=%s",
                            gson.toJson(bindingResult.getFieldErrors())), AuditReasonEnum.AUTHORIZED);
            return buildValidationResult(bindingResult);
        }
        param.cleanSelf();

        // 检验是否拥有云账号管理权限
        authService.interceptFirstLogin(userDo);
        authService.checkAccountManagerPermission(userDo);
        if (userDo.getLoginName().equals(param.getAliyunId())) {
            return PrivateResultBuilder.buildExtBadRequestResult(
                    locale.msg("private.account.lock.badRequest.selfModified"));
        }

        // 校验通过，执行对应的锁定和解锁操作
        accountService.lock(param.getAliyunId(), param.getIsLock());
        auditUtil.info(userDo, AuditTargetEnum.DATA, AuditActionEnum.UPDATE, AuditOutcomeEnum.SUCCESS,
                String.format("authproxy - lock/unlock account, param=%s", gson.toJson(param)),
                AuditReasonEnum.AUTHORIZED);
        return PrivateResultBuilder.buildExtSuccessResult();
    }

    /**
     * 云账号删除 API
     */
    @RequestMapping(value = "delete", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public TeslaResult accountDelete(@Valid @RequestBody PrivateAccountDeleteParam param,
                                     BindingResult bindingResult, HttpServletRequest request)
            throws PrivateAuthNotLogin, PrivateAuthForbidden, AuthProxyThirdPartyError, PrivateValidationError {
        UserDO userDo = (UserDO) request.getAttribute(Constants.REQUEST_ATTR_USER);
        if (bindingResult.hasErrors()) {
            auditUtil.info(userDo, AuditTargetEnum.DATA, AuditActionEnum.DELETE, AuditOutcomeEnum.FAILURE,
                    String.format("authproxy - delete account, validationError=%s",
                            gson.toJson(bindingResult.getFieldErrors())), AuditReasonEnum.AUTHORIZED);
            return buildValidationResult(bindingResult);
        }
        param.cleanSelf();

        // 检验是否拥有云账号管理权限
        authService.interceptFirstLogin(userDo);
        authService.checkAccountManagerPermission(userDo);

        // 校验通过，进行账户删除
        if (userDo.getLoginName().equals(param.getAliyunId())) {
            throw new PrivateValidationError(
                    "aliyunId", locale.msg("private.account.delete.validation.aliyunId.selfDelete"));
        }
        accountService.delete(param);
        auditUtil.info(userDo, AuditTargetEnum.DATA, AuditActionEnum.DELETE, AuditOutcomeEnum.SUCCESS,
                String.format("authproxy - delete account, param=%s", gson.toJson(param)), AuditReasonEnum.AUTHORIZED);
        return PrivateResultBuilder.buildExtSuccessResult();
    }

    /**
     * 云账号信息修改 API
     */
    @RequestMapping(value = "validation", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public TeslaResult accountValidation(@Valid @RequestBody PrivateAccountValidationParam param,
                                         BindingResult bindingResult, HttpServletRequest request)
            throws PrivateAuthNotLogin, PrivateAuthForbidden, AuthProxyThirdPartyError, PrivateValidationError {
        UserDO userDo = (UserDO) request.getAttribute(Constants.REQUEST_ATTR_USER);
        if (bindingResult.hasErrors()) {
            auditUtil.info(userDo, AuditTargetEnum.DATA, AuditActionEnum.UPDATE, AuditOutcomeEnum.FAILURE,
                    String.format("authproxy - update validation info, validationError=%s",
                            gson.toJson(bindingResult.getFieldErrors())),
                    AuditReasonEnum.AUTHORIZED);
            return buildValidationResult(bindingResult);
        }
        param.cleanSelf();

        // 检验是否拥有云账号管理权限
        authService.interceptFirstLogin(userDo);
        authService.checkAccountManagerPermission(userDo);

        // 校验通过，进行密码修改
        PrivateAccountValidationResult result = accountService.validation(param);
        auditUtil.info(userDo, AuditTargetEnum.DATA, AuditActionEnum.UPDATE, AuditOutcomeEnum.SUCCESS,
                String.format("authproxy - update validation info, param=%s, result=%s", gson.toJson(param),
                        gson.toJson(result)), AuditReasonEnum.AUTHORIZED);
        return PrivateResultBuilder.buildExtSuccessResult(result);
    }

}
