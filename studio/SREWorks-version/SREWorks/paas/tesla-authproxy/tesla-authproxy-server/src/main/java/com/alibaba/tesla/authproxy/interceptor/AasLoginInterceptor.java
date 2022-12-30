package com.alibaba.tesla.authproxy.interceptor;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.authproxy.ApplicationException;
import com.alibaba.tesla.authproxy.AuthProperties;
import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.lib.exceptions.AuthProxyThirdPartyError;
import com.alibaba.tesla.authproxy.lib.exceptions.PrivateAasVerifyFailed;
import com.alibaba.tesla.authproxy.lib.exceptions.PrivateAuthNotLogin;
import com.alibaba.tesla.authproxy.model.mapper.AppMapper;
import com.alibaba.tesla.authproxy.model.AppDO;
import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.outbound.aas.AasClient;
import com.alibaba.tesla.authproxy.outbound.aas.AasUserInfo;
import com.alibaba.tesla.authproxy.service.TeslaUserService;
import com.alibaba.tesla.authproxy.util.*;
import com.alibaba.tesla.common.utils.TeslaResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * <p>Title: AasLoginInterceptor.java<／p>
 * <p>Description: AAS登录认证拦截处理 <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
@Component("aasLoginInterceptor")
@Slf4j
public class AasLoginInterceptor implements LoginInterceptor {

    @Autowired
    private TeslaUserService teslaUserService;

    @Autowired
    private AppMapper appMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private AasClient aasClient;

    @Autowired
    private AuthUtil authUtil;

    @Autowired
    private LocaleUtil locale;

    @Autowired
    private AuthProperties authProperties;

    private Gson gson = new GsonBuilder().serializeNulls().create();

    /**
     * AAS 登录拦截处理逻辑
     */
    @Override
    public boolean interceptor(HttpServletRequest request, HttpServletResponse response) throws PrivateAuthNotLogin {
        /**
         * 优先从cookie中获取用户信息
         */
//        Cookie teslaTokenCookie = CookieUtil.getCookie(request, AuthProxyConstants.COOKIE_SSO_LOGIN_TOKEN);
//        if (teslaTokenCookie != null) {
//            String teslaToken = teslaTokenCookie.getValue();
//            Claims claims = null;
//            try {
//                claims = TeslaJwtUtil.verify(teslaToken, authProperties.getOauth2JwtSecret());
//                String empId = claims.get(AuthJwtConstants.JWT_EMP_ID_CLAIM_KEY, String.class);
//                UserDO userDo = teslaUserService.getUserByEmpId(empId);
//                if (userDo != null) {
//                    log.info("Tesla cookie auth success:{}", JSONObject.toJSONString(userDo));
//                    request.setAttribute(Constants.REQUEST_ATTR_USER, userDo);
//                    return true;
//                }
//            } catch (TeslaJwtException e) {
//                log.warn("Verify tesla jwt token failed! ErrMsg:{}", e.getMessage());
//            }
//        }

        // 检查是否存在合法的第三方应用 Header，存在则直接放行
        UserDO teslaUser = authUtil.getExtAppUser(request);
        if (null != teslaUser) {
            log.info("Get user info from ext app, user={}", gson.toJson(teslaUser));
            request.setAttribute(Constants.REQUEST_ATTR_USER, teslaUser);
            return true;
        }

        // 从 Cookie 中获取 AAS Login Ticket
        Cookie cookie = CookieUtil.getCookie(request, Constants.COOKIE_AAS_LOGIN_TICKET);
        if (null == cookie) {
            log.info("No aas login ticket found, not login, requestCookie={}", CookieUtil.cookieString(request));
            throw new PrivateAuthNotLogin(locale.msg("private.exception.authForbidden.notLogin"));
        }

        // 如果缓存中已经存在该 AAS Login Ticket 的相关记录且和数据库一致，那么直接放行
        String loginAliyunidTicket = cookie.getValue();
        String ticketCacheKey = RedisUtil.buildPrivateTicketKey(loginAliyunidTicket);
        String noCache = request.getParameter("noCache");
        if (null == noCache || noCache.equals("0")) {
            String loginName = redisTemplate.opsForValue().get(ticketCacheKey);
            if (null != loginName) {
                teslaUser = teslaUserService.getUserByLoginName(loginName);
                if (null != teslaUser) {
                    if (teslaUser.getIsLocked().intValue() == 1) {
                        throw new PrivateAuthNotLogin(locale.msg("private.exception.authForbidden.notLogin"));
                    }
                    request.setAttribute(Constants.REQUEST_ATTR_USER, teslaUser);
                    return true;
                }
            }
        }

        // 获取对应的 Login URL
        String loginUrl = buildLoginUrl(request);
        log.debug("Intercept url: {}, login url: {}", request.getRequestURI(), loginUrl);

        // 从 AAS 处获取用户信息
        AasUserInfo responseUser;
        try {
            responseUser = aasClient.loadUserInfo(loginAliyunidTicket);
        } catch (PrivateAasVerifyFailed e) {
            throw new PrivateAuthNotLogin(locale.msg("private.exception.authForbidden.cookie"));
        } catch (AuthProxyThirdPartyError e) {
            throw new PrivateAuthNotLogin(locale.msg("private.exception.authForbidden.thirdpartyError"));
        }
        log.info("Get user info from aas, responseUser={}", gson.toJson(responseUser));

        // 将用户写入 DB
        Date now = new Date();
        teslaUser = new UserDO();
        teslaUser.setTenantId(Constants.DEFAULT_TENANT_ID);
        teslaUser.setEmail(responseUser.getEmail());
        teslaUser.setLoginName(responseUser.getAliyunID());
        teslaUser.setBid(authProperties.getAasDefaultBid());
        teslaUser.setEmpId(responseUser.getAliyunPK());
        teslaUser.setNickName(
            null == responseUser.getNickName() || responseUser.getNickName().length() == 0 ? responseUser
                .getAliyunID() : responseUser.getNickName());
        teslaUser.setAliyunPk(responseUser.getAliyunPK());
        teslaUser.setLastLoginTime(now);
        teslaUser.setLang(authProperties.getDefaultLanguage());
        teslaUser.setGmtCreate(now);
        teslaUser.setGmtModified(now);
        teslaUser.setUserId(UserUtil.getUserId(teslaUser));
        log.info("Login success，save login user {}", JSONObject.toJSON(teslaUser));
        teslaUserService.save(teslaUser);

        // 写入用户登录信息到缓存
        if (null == noCache || noCache.equals("0")) {
            redisTemplate.opsForValue().set(ticketCacheKey, responseUser.getAliyunID(), 86400, TimeUnit.SECONDS);
        }
        request.setAttribute(Constants.REQUEST_ATTR_USER, teslaUser);
        return true;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, String callback) {
        CookieUtil.cleanLoginCookie(request, response, Constants.COOKIE_AAS_LOGIN_TICKET);
        CookieUtil.cleanLoginCookie(request, response, Constants.COOKIE_AAS_LOGIN_USER_ID);
        CookieUtil.cleanLoginCookie(request, response, Constants.COOKIE_AAS_LOGIN_ALIYUNID);
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
     * 获取当前的登录地址
     */
    private String buildLoginUrl(HttpServletRequest request) {
        String appId = request.getParameter("appId");
        if (null == appId) {
            appId = "unknown";
        }
        AppDO appDo = appMapper.getByAppId(appId);

        String referer = request.getParameter("callbackUrl");
        if (null == referer) {
            referer = request.getHeader(HttpHeaders.REFERER);
        }
        if (StringUtil.isEmpty(referer)) {
            // 返回指定 app 的主页
            if (null != appDo) {
                referer = appDo.getIndexUrl();
            }
            if (StringUtil.isEmpty(referer)) {
                referer = authProperties.getCallbackUrl();
            }
        }

        // 生成编码后的 Referer URL
        String encodingRefererUrl;
        try {
            encodingRefererUrl = URLEncoder.encode(referer, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.warn("Unsupported http referer url, appId={}, referer={}",
                request.getParameter("appId"), request.getHeader(HttpHeaders.REFERER));
            try {
                encodingRefererUrl = URLEncoder.encode(authProperties.getCallbackUrl(), "UTF-8");
            } catch (UnsupportedEncodingException e1) {
                log.error("Invalid callbackUrl config {}", authProperties.getCallbackUrl());
                encodingRefererUrl = "unknown";
            }
        }

        // 生成 Login URL
        if (null == appDo ||
            null == appDo.getLoginUrl() ||
            appDo.getLoginUrl().length() == 0 ||
            appDo.getLoginUrl().equals(authProperties.getLoginUrl())) {
            return authProperties.getLoginUrl() + "?oauth_callback=" + encodingRefererUrl;
        } else {
            return appDo.getLoginUrl() + "?callback=" + encodingRefererUrl;
        }
    }
}