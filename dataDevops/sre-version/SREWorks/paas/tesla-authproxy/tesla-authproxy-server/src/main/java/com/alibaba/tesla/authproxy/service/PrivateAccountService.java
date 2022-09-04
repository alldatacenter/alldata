package com.alibaba.tesla.authproxy.service;

import com.alibaba.tesla.authproxy.lib.exceptions.AuthProxyThirdPartyError;
import com.alibaba.tesla.authproxy.lib.exceptions.PrivateInternalError;
import com.alibaba.tesla.authproxy.lib.exceptions.PrivateValidationError;
import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.outbound.aas.AasLoginResult;
import com.alibaba.tesla.authproxy.web.input.*;
import com.alibaba.tesla.authproxy.web.output.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;

/**
 * 专有云用户服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface PrivateAccountService {

    PrivateAccountAliyunResult getAliyunPkById(String aliyunId) throws PrivateValidationError;

    PrivateAccountAliyunResult getAliyunIdByPk(String aliyunPk) throws PrivateValidationError;

    PrivateAccountAliyunListResult getAliyunPkByIds(String aliyunIds) throws PrivateValidationError;

    PrivateAccountAliyunListResult getAliyunIdByPks(String aliyunPks) throws PrivateValidationError;

    PrivateAccountListResult accountListBySearch(String search);

    PrivateAccountListResult accountListByUser(UserDO userDo);

    PrivateAccountAddResult accountAdd(PrivateAccountAddParam param, HttpServletRequest request)
            throws AuthProxyThirdPartyError, PrivateValidationError, PrivateInternalError;

    void passwordChange(String aliyunId, String password) throws PrivateValidationError, AuthProxyThirdPartyError;

    void lock(String aliyunId, Boolean isLock) throws PrivateValidationError, AuthProxyThirdPartyError;

    void infoChange(PrivateAccountInfoChangeParam param, HttpServletRequest request) throws PrivateValidationError;

    void delete(PrivateAccountDeleteParam param) throws PrivateValidationError, AuthProxyThirdPartyError;

    PrivateAccountValidationResult validation(PrivateAccountValidationParam param) throws PrivateValidationError;

    PrivateAccountLoginOptionResult getLoginOption(String aliyunId);

    PrivateAccountLoginSmsResult loginSms(PrivateAccountLoginSmsParam param, HttpSession session)
            throws PrivateValidationError, AuthProxyThirdPartyError;

    AasLoginResult login(PrivateAccountLoginParam param, HttpSession session)
            throws PrivateValidationError, AuthProxyThirdPartyError;

    LocalDateTime getUserNextPasswordChangeDate(String aliyunId);

    void checkLoginStatus(AasLoginResult result) throws PrivateValidationError;
}
