package com.alibaba.tesla.authproxy.interceptor;

import com.alibaba.tesla.authproxy.ApplicationException;
import com.alibaba.tesla.authproxy.AuthProperties;
import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.lib.exceptions.AliyunAuthNotLogin;
import com.alibaba.tesla.authproxy.lib.exceptions.AuthProxyException;
import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.model.vo.AliyunRefreshTokenVO;
import com.alibaba.tesla.authproxy.model.vo.AliyunUserInfoVO;
import com.alibaba.tesla.authproxy.service.TeslaUserService;
import com.alibaba.tesla.authproxy.util.*;
import com.alibaba.tesla.common.base.util.TeslaGsonUtil;
import com.alibaba.tesla.common.utils.TeslaResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 阿里云账号登陆拦截器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Component("aliyunLoginInterceptor")
@Slf4j
public class AliyunLoginInterceptor implements LoginInterceptor {

    @Autowired
    private AuthUtil authUtil;

    @Autowired
    private AliyunOauth2Util aliyunOauth2Util;

    @Autowired
    private LocaleUtil localeUtil;

    @Autowired
    private AuthProperties authProperties;

    @Autowired
    private TeslaUserService teslaUserService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 登录拦截处理逻辑
     */
    @Override
    public boolean interceptor(HttpServletRequest request, HttpServletResponse response) throws AuthProxyException {
        // 检查是否存在合法的第三方应用 Header，存在则直接放行
        UserDO teslaUser = authUtil.getExtAppUser(request);
        if (null != teslaUser) {
            log.info("Get user info from ext app, user={}", TeslaGsonUtil.toJson(teslaUser));
            request.setAttribute(Constants.REQUEST_ATTR_USER, teslaUser);
            return true;
        }

        // 从 Cookie 中获取当前的 AccessToken, RefreshToken, ExpiresAt
        Cookie accessTokenCookie = CookieUtil.getCookie(request, Constants.COOKIE_ALIYUN_ACCESS_TOKEN);
        Cookie refreshTokenCookie = CookieUtil.getCookie(request, Constants.COOKIE_ALIYUN_REFRESH_TOKEN);
        Cookie expiresAtCookie = CookieUtil.getCookie(request, Constants.COOKIE_ALIYUN_EXPIRES_AT);
        if (null == accessTokenCookie || null == refreshTokenCookie || null == expiresAtCookie ||
            StringUtil.isEmpty(accessTokenCookie.getValue()) ||
            StringUtil.isEmpty(refreshTokenCookie.getValue()) ||
            StringUtil.isEmpty(expiresAtCookie.getValue())) {
            log.info("No aliyun access token found, requestCookie={}", CookieUtil.cookieString(request));
            throw new AliyunAuthNotLogin(localeUtil.msg("aliyun.exception.authForbidden.notLogin"));
        }
        String accessToken = accessTokenCookie.getValue();
        String refreshToken = refreshTokenCookie.getValue();
        Long expiresAt = Long.valueOf(expiresAtCookie.getValue());

        // 如果 Access Token 即将过期，那么根据 Refresh Token 进行续期
        Long now = System.currentTimeMillis() / Constants.MILLS_SECS;
        if (expiresAt - now < Constants.OAUTH2_EXPIRE_GAP) {
            AliyunRefreshTokenVO refreshTokenVO = aliyunOauth2Util.getRefreshToken(refreshToken);
            log.info("Get refresh token, origin access token is {}, new access token {}",
                accessTokenCookie.getValue(), TeslaGsonUtil.toJson(refreshTokenVO));
            accessToken = refreshTokenVO.getAccessToken();
            expiresAt = now + Long.parseLong(refreshTokenVO.getExpiresIn());
            String topDomain = CookieUtil.getCookieDomain(request, authProperties.getCookieDomain());
            CookieUtil.setCookie(response, Constants.COOKIE_ALIYUN_ACCESS_TOKEN, accessToken, 0, topDomain);
            CookieUtil.setCookie(response, Constants.COOKIE_ALIYUN_EXPIRES_AT, String.valueOf(expiresAt), 0, topDomain);
        }

        // 从 redis 中获取该 access token 对应的用户信息
        String accessTokenCacheKey = RedisUtil.buildPrivateTicketKey(accessToken);
        String noCache = request.getParameter("noCache");
        if (null == noCache || "0".equals(noCache)) {
            String loginName = redisTemplate.opsForValue().get(accessTokenCacheKey);
            if (null != loginName) {
                teslaUser = teslaUserService.getUserByLoginName(loginName);
                if (null != teslaUser) {
                    request.setAttribute(Constants.REQUEST_ATTR_USER, teslaUser);
                    return true;
                }
            }
        }

        // 缓存中无用户信息，那么从阿里云接口中获取对应的用户信息
        AliyunUserInfoVO userInfo = aliyunOauth2Util.getUserInfo(accessToken);
        log.info("Get user info from aliyun, userInfo={}, accessToken={}", TeslaGsonUtil.toJson(userInfo), accessToken);
        String loginName = userInfo.getLoginName();
        String upn = userInfo.getUpn();
        String uid = userInfo.getUid();
        if (loginName != null && uid != null) {
            // 当 loginName 存在的时候，说明是使用的主账号登陆
            teslaUser = saveUser(loginName, uid);
        } else if (upn != null && uid != null) {
            // 当 upm 且 uid 存在的时候，说明是使用的 RAM 子账号登陆
            teslaUser = saveUser(upn, uid);
        } else {
            log.info("Cannot get valid user info from aliyun access token, forbidden, accessToken={}", accessToken);
            return false;
        }

        // 写入缓存中并返回
        redisTemplate.opsForValue().set(accessTokenCacheKey, teslaUser.getLoginName(), 86400, TimeUnit.SECONDS);
        request.setAttribute(Constants.REQUEST_ATTR_USER, teslaUser);
        return true;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, String callback) {
        String topDomain = CookieUtil.getCookieDomain(request, authProperties.getCookieDomain());
        CookieUtil.cleanDomainCookie(request, response, Constants.COOKIE_ALIYUN_ACCESS_TOKEN, topDomain, "/");
        CookieUtil.cleanDomainCookie(request, response, Constants.COOKIE_ALIYUN_REFRESH_TOKEN, topDomain, "/");
        CookieUtil.cleanDomainCookie(request, response, Constants.COOKIE_ALIYUN_EXPIRES_AT, topDomain, "/");
        // 重定向到登出URL
        if (null == authProperties.getLogoutUrl() || authProperties.getLogoutUrl().length() == 0) {
            log.error("${logout.url} config is empty please set.");
            throw new ApplicationException(TeslaResult.FAILURE, "error.config.logoutUrl");
        }
        String callBackUrl = authProperties.getLogoutUrl() + "?oauth_callback=" + callback;
        if (log.isDebugEnabled()) {
            log.debug("Logout success，redirect to page {}", callBackUrl);
        }
        try {
            response.sendRedirect(callBackUrl);
        } catch (IOException ignored) {}
    }

    /**
     * 根据 aliyunId 和 aliyunPk 保存用户，仅当不存在时新增
     *
     * @param aliyunId 阿里云 ID (login_name)
     * @param aliyunPk 阿里云主键
     */
    private UserDO saveUser(String aliyunId, String aliyunPk) {
        Date now = new Date();
        UserDO teslaUser = new UserDO();
        teslaUser.setTenantId(Constants.DEFAULT_TENANT_ID);
        teslaUser.setEmail(aliyunId);
        teslaUser.setLoginName(aliyunId);
        teslaUser.setBid(authProperties.getAasDefaultBid());
        teslaUser.setEmpId(aliyunPk);
        teslaUser.setNickName(aliyunId);
        teslaUser.setAliyunPk(aliyunPk);
        teslaUser.setLastLoginTime(now);
        teslaUser.setLang(authProperties.getDefaultLanguage());
        teslaUser.setGmtCreate(now);
        teslaUser.setGmtModified(now);
        teslaUser.setUserId(UserUtil.getUserId(teslaUser));
        teslaUserService.save(teslaUser);
        return teslaUser;
    }
}
