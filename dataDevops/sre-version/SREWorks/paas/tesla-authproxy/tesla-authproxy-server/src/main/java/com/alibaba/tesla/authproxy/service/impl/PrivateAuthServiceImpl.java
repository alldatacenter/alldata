package com.alibaba.tesla.authproxy.service.impl;

import com.alibaba.tesla.authproxy.AuthProperties;
import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.lib.exceptions.AuthProxyThirdPartyError;
import com.alibaba.tesla.authproxy.lib.exceptions.PrivateAuthForbidden;
import com.alibaba.tesla.authproxy.lib.exceptions.PrivateAuthNotLogin;
import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.outbound.oam.OamClient;
import com.alibaba.tesla.authproxy.service.PrivateAuthService;
import com.alibaba.tesla.authproxy.service.TeslaUserService;
import com.alibaba.tesla.authproxy.util.LocaleUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 专有云验证服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
public class PrivateAuthServiceImpl implements PrivateAuthService {

    @Autowired
    private OamClient oamClient;

    @Autowired
    private TeslaUserService teslaUserService;

    @Autowired
    private LocaleUtil locale;

    @Autowired
    private AuthProperties authProperties;

    /**
     * 根据传入的 Cookie 获取当前对应的用户，并检查是否拥有账户管理权限
     *
     * @param userDo 用户实例
     * @return UserDO 对象
     */
    @Override
    public UserDO checkAccountManagerPermission(UserDO userDo)
            throws PrivateAuthForbidden, PrivateAuthNotLogin, AuthProxyThirdPartyError {
        String accountResourceName = authProperties.getOamAccountResourceName();
        if (null == userDo) {
            throw new PrivateAuthForbidden(locale.msg("private.exception.authForbidden"), accountResourceName);
        }

        String aliyunPk = userDo.getAliyunPk();
        String bid = userDo.getBid();
        Boolean checkResult = Boolean.FALSE;
        UserDO adminUser = getAdminRoleUserDo(aliyunPk, bid);
        //try {
        //    checkResult = oamClient.checkPermission(adminUser.getAliyunPk(), adminUser.getBid(), aliyunPk,
        //            accountResourceName);
        //} catch (ServerException e) {
        //    checkResult = Boolean.FALSE;
        //} catch (ClientException e) {
        //    switch (e.getErrCode()) {
        //        case "USER_NOT_EXIST":
        //            break;
        //        default:
        //            AuthProxyThirdPartyError se = new AuthProxyThirdPartyError(Constants.THIRD_PARTY_OAM, e.getMessage());
        //            se.initCause(e);
        //            throw se;
        //    }
        //}
        if (!checkResult) {
            throw new PrivateAuthForbidden(locale.msg("private.exception.authForbidden"), accountResourceName);
        }
        return userDo;
    }

    /**
     * 拦截首次登录用户
     *
     * @param userDo 用户实例
     */
    @Override
    public void interceptFirstLogin(UserDO userDo) throws PrivateAuthForbidden {
        if (null == userDo) {
            throw new PrivateAuthForbidden(locale.msg("private.exception.authForbidden.firstLogin"));
        }
        if (userDo.getIsFirstLogin().intValue() == 1) {
            throw new PrivateAuthForbidden(locale.msg("private.exception.authForbidden.firstLogin"));
        }
    }

    /**
     * 获取 bcc_admin 管理角色的拥有者用户
     *
     * @param aliyunPk 当前用户 aliyun pk
     * @param bid      当前用户 bid
     */
    private UserDO getAdminRoleUserDo(String aliyunPk, String bid)
            throws AuthProxyThirdPartyError, PrivateAuthForbidden {
        throw new RuntimeException("not support");
        //GetRoleResponse.Data data;
        //try {
        //    data = oamClient.getRole(aliyunPk, bid, authProperties.getOamAdminRole());
        //} catch (ClientException e) {
        //    switch (e.getErrCode()) {
        //        case "USER_NOT_EXIST":
        //            try {
        //                oamClient.getOamUserByUsername(aliyunPk);
        //                data = oamClient.getRole(aliyunPk, bid, authProperties.getOamAdminRole());
        //            } catch (ClientException ie) {
        //                AuthProxyThirdPartyError se = new AuthProxyThirdPartyError(Constants.THIRD_PARTY_OAM,
        //                        "GetOamUserByUserName/GetRole failed: " + ie.getMessage());
        //                se.initCause(ie);
        //                throw se;
        //            }
        //            break;
        //        default:
        //            AuthProxyThirdPartyError se = new AuthProxyThirdPartyError(Constants.THIRD_PARTY_OAM,
        //                    "GetRole failed: " + e.getMessage());
        //            se.initCause(e);
        //            throw se;
        //    }
        //
        //}
        //return teslaUserService.getUserByAliyunPk(data.getOwnerName());
    }
}
