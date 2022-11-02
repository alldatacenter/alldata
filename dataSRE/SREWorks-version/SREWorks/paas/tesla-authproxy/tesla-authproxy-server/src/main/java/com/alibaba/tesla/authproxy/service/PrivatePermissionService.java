package com.alibaba.tesla.authproxy.service;

import com.alibaba.tesla.authproxy.lib.exceptions.AuthProxyThirdPartyError;
import com.alibaba.tesla.authproxy.lib.exceptions.PrivateInternalError;
import com.alibaba.tesla.authproxy.lib.exceptions.PrivateValidationError;
import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.service.constant.UserTypeEnum;
import com.alibaba.tesla.authproxy.web.input.PrivatePermissionRoleAccountAddParam;
import com.alibaba.tesla.authproxy.web.input.PrivatePermissionRoleAccountDeleteParam;
import com.alibaba.tesla.authproxy.web.output.PrivatePermissionRoleAccountResult;
import com.alibaba.tesla.authproxy.web.output.PrivatePermissionRoleResult;

/**
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface PrivatePermissionService {

    PrivatePermissionRoleResult getRoles(UserDO userDo) throws AuthProxyThirdPartyError, PrivateInternalError;

    PrivatePermissionRoleResult getApiRoles(UserDO userDo) throws AuthProxyThirdPartyError,
        PrivateInternalError;

    PrivatePermissionRoleAccountResult getRoleAccounts(UserDO userDo, String roleName, String filter)
            throws AuthProxyThirdPartyError, PrivateValidationError, PrivateInternalError;

    void grantRoleToAccount(UserDO userDo, PrivatePermissionRoleAccountAddParam param)
            throws AuthProxyThirdPartyError, PrivateValidationError, PrivateInternalError;

    void deleteRoleFromAccount(UserDO userDo, PrivatePermissionRoleAccountDeleteParam param)
            throws AuthProxyThirdPartyError, PrivateValidationError, PrivateInternalError;

    UserTypeEnum getUserType(UserDO userDo) throws AuthProxyThirdPartyError, PrivateInternalError;
}
