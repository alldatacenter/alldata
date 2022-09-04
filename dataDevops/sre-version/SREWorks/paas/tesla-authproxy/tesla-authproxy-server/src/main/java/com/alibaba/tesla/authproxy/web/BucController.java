package com.alibaba.tesla.authproxy.web;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.tesla.authproxy.AuthProperties;
import com.alibaba.tesla.authproxy.BaseController;
import com.alibaba.tesla.authproxy.constants.AuthProxyConstants;
import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.service.TeslaUserService;
import com.alibaba.tesla.authproxy.service.UserService;
import com.alibaba.tesla.authproxy.util.CookieUtil;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.common.base.TeslaResultFactory;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 权代服务请求处理基础Controller
 * <p>
 * 处理请求：SSO登录成功回调
 * <p>
 * <p>Title: AuthProxyBucController.java<／p>
 * <p>Description: <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
@Controller
@RequestMapping("auth/buc")
@Slf4j
public class BucController extends BaseController {

    private static final String ORIGIN_BACK_URL_KEY = "origin_back_url";
    private static final int JWT_TOKEN_TIMEOUT = 86400000;

    @Autowired
    private AuthProperties authProperties;

    @Autowired
    private TeslaUserService teslaUserService;

    @Autowired
    private UserService userService;


    @RequestMapping("/login")
    @ResponseBody
    public void login(@RequestParam(name = "front_back_url", required = false) String frontBackUrl,
        @RequestParam(name = "back_url", required = false) String backUrl,
        @RequestParam(name = "backurl", required = false) String backurl,
        HttpServletResponse response) throws URISyntaxException, IOException {
        String originBackURL = parseOriginalBackUrl(frontBackUrl, backUrl, backurl);
        URI uri = new URIBuilder(authProperties.getServerUrl() + authProperties.getTeslaLoginUrl())
            .addParameter(ORIGIN_BACK_URL_KEY, originBackURL).build();
        log.info("BucLogin:login,redirect to SSO server, backUrl: {}", uri.toString());
        String bucLoginUrl = String.format("%s/ssoLogin.htm", authProperties.getSsoServerUrl());
        URI bucUri = new URIBuilder(bucLoginUrl)
            .addParameter("APP_NAME", authProperties.getAppName())
            .addParameter("BACK_URL", uri.toString())
            .addParameter("CONTEXT_PATH", authProperties.getContextPath())
            .addParameter("CLIENT_VERSION", authProperties.getClientVersion()).build();
        response.sendRedirect(bucUri.toString());
    }

    @RequestMapping("/logout")
    @ResponseBody
    public TeslaBaseResult logout(@RequestParam(name = "front_back_url", required = false) String frontBackUrl,
        @RequestParam(name = "back_url", required = false) String backUrl,
        @RequestParam(name = "backurl", required = false) String backurl,
        HttpServletRequest request,
        HttpServletResponse response) throws URISyntaxException, IOException {
        UserDO userDo = this.getLoginUser(request);
        if (null == userDo) {
            return TeslaResultFactory.buildClientErrorResult("You are not logined yet, can't logout!");
        }
        request.getSession().invalidate();
        String topDomain = CookieUtil.getCookieDomain(request, authProperties.getCookieDomain());
        CookieUtil.cleanDomainCookie(request, response, AuthProxyConstants.COOKIE_SSO_LOGIN_TOKEN, topDomain, "/");
        String originBackURL = parseOriginalBackUrl(frontBackUrl, backUrl, backurl);
        String bucLoginUrl = String.format("%s/ssoLogout.htm", authProperties.getSsoServerUrl());
        URI bucUri = new URIBuilder(bucLoginUrl)
            .addParameter("APP_NAME", authProperties.getAppName())
            .addParameter("BACK_URL", originBackURL)
            .addParameter("CONTEXT_PATH", authProperties.getContextPath())
            .addParameter("CLIENT_VERSION", authProperties.getClientVersion()).build();
        response.sendRedirect(bucUri.toString());
        return TeslaResultFactory.buildSucceedResult("");
    }

    /**
     * Process sso logout callback, clean cookie.This API will be invoked by BUC, when other systems logout.
     *
     * @param request
     * @param response
     */
    @RequestMapping(value = "/bucSSOLogout.do", method = RequestMethod.GET)
    public void bucSSOLogout(HttpServletRequest request, HttpServletResponse response) {
        log.info("Process sso logout callback, clean cookie.");
        String topDomain = CookieUtil.getCookieDomain(request, authProperties.getCookieDomain());
        CookieUtil.cleanDomainCookie(request, response, AuthProxyConstants.COOKIE_SSO_LOGIN_TOKEN, topDomain);
        CookieUtil.cleanDomainCookie(request, response, "LAST_HEARTBEAT_TIME", topDomain);
    }

    private String parseOriginalBackUrl(String frontBackUrl, String backUrl, String backurl) {
        if (!StringUtils.isBlank(backurl)) {
            return backurl;
        }
        if (!StringUtils.isBlank(backUrl)) {
            return backUrl;
        }
        if (!StringUtils.isBlank(frontBackUrl)) {
            return frontBackUrl;
        }
        return "";
    }
}