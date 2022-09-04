package com.alibaba.tesla.authproxy.web;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.authproxy.ApplicationException;
import com.alibaba.tesla.authproxy.AuthProperties;
import com.alibaba.tesla.authproxy.BaseController;
import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.component.cookie.ResponseCookie;
import com.alibaba.tesla.authproxy.constants.AuthProxyConstants;
import com.alibaba.tesla.authproxy.lib.exceptions.DataBaseValidationError;
import com.alibaba.tesla.authproxy.lib.exceptions.PrivateValidationError;
import com.alibaba.tesla.authproxy.model.AppDO;
import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.model.vo.LoginUserInfoVO;
import com.alibaba.tesla.authproxy.service.TeslaAppService;
import com.alibaba.tesla.authproxy.service.TeslaUserService;
import com.alibaba.tesla.authproxy.util.*;
import com.alibaba.tesla.authproxy.web.input.DataBaseAccountLoginParam;
import com.alibaba.tesla.common.utils.TeslaResult;
import com.alibaba.tesla.common.utils.TeslaResultBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * 权代服务请求处理基础Controller
 * <p>
 * 处理请求： 1、Aone部署自检服务 2、服务启动初始 3、注销登录
 * <p>
 * <p>Title: AuthProxyController.java<／p>
 * <p>Description: <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
@Controller
@Slf4j
public class LoginController extends BaseController {

    @Autowired
    private AuthProperties authProperties;

    @Autowired
    private TeslaAppService teslaAppService;

    @Autowired
    private TeslaUserService teslaUserService;

    /**
     * DataBase认证方式登陆接口
     *
     * @param param
     * @param bindingResult
     * @param request
     * @param response
     * @return
     * @throws PrivateValidationError
     */
    @RequestMapping(value = "auth/login", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public TeslaResult login(@Valid @RequestBody DataBaseAccountLoginParam param, BindingResult bindingResult,
                             HttpServletRequest request, HttpServletResponse response)
        throws DataBaseValidationError, PrivateValidationError {
        if (bindingResult.hasErrors()) {
            return buildValidationResult(bindingResult);
        }
        param.cleanSelf();

        //每次点击登录先清理cookie
        String topDomain = CookieUtil.getCookieDomain(request, authProperties.getCookieDomain());
        CookieUtil.cleanDomainCookie(request, response, Constants.COOKIE_DATABASE_LOGIN_TOKEN, topDomain, "/");
        CookieUtil.cleanDomainCookie(request, response, Constants.COOKIE_DATABASE_LOGIN_USER_ID, topDomain, "/");
        CookieUtil.cleanDomainCookie(request, response, Constants.COOKIE_LANG, topDomain, "/");
        CookieUtil.cleanDomainCookie(request, response, Constants.COOKIE_COUNTRY, topDomain, "/");

        // 根据提供的参数进行登录
        UserDO userDo = teslaUserService.getUserByLoginNameAndPwd(param.getLoginName(), param.getPassword());
        if (null == userDo) {
            return buildFailedTeslaResult("Incorrect login name or password");
        }

        //写入tesla token cookie
        String teslaToken = TeslaJwtUtil.create(userDo.getEmpId(), userDo.getLoginName(),
            Objects.toString(userDo.getBucId()), userDo.getEmail(),
            UserUtil.getUserId(userDo), userDo.getNickName(), userDo.getAliyunPk(), TeslaJwtUtil.JWT_TOKEN_TIMEOUT,
            authProperties.getOauth2JwtSecret());
        if (Objects.equals(authProperties.getNetworkProtocol(), "https")) {
            ResponseCookie responseCookie = ResponseCookie.builder().name(AuthProxyConstants.COOKIE_SSO_LOGIN_TOKEN).value(teslaToken)
                .maxAge(Duration.ofSeconds(24 * 60 * 60))
                .domain(authProperties.getCookieDomain())
                .sameSite("None")
                .secure(true)
                .path("/")
                .build();
            response.addHeader("Set-Cookie", responseCookie.toString());
        } else {
            CookieUtil.setCookie(response, AuthProxyConstants.COOKIE_SSO_LOGIN_TOKEN, teslaToken, 0);
        }

        // 登录成功后生成Cookie写入当前 response 中
        try {
            CookieUtil.setCookie(response, Constants.COOKIE_DATABASE_LOGIN_USER_ID, userDo.getLoginName(),
                Constants.DEFAULT_COOKIE_TIME, topDomain);
            /**
             * token的时间设置比cookie稍微长一点，防止cookie校验通过后token验证失败。
             */
            String token = JwtUtil.create(userDo.getLoginName(), Constants.DEFAULT_COOKIE_TIME + (120 * 1000),
                authProperties.getOauth2JwtSecret());
            CookieUtil.setCookie(response, Constants.COOKIE_DATABASE_LOGIN_TOKEN, token, Constants.DEFAULT_COOKIE_TIME,
                topDomain);
        } catch (Exception e) {
            log.error("##### write cookie faied", e);
            return buildFailedTeslaResult("cookie write failed: " + e.getLocalizedMessage());
        }
        log.info("database login success, loginUser={}", JSONObject.toJSONString(userDo));

        // 设置对应的语言
        String lang = param.getLang();
        String[] langs = lang.split("_");
        if (langs.length == 2) {
            CookieUtil.setCookie(response, Constants.COOKIE_LANG, langs[0], Constants.DEFAULT_COOKIE_TIME, topDomain);
            CookieUtil.setCookie(response, Constants.COOKIE_COUNTRY, langs[1], Constants.DEFAULT_COOKIE_TIME,
                topDomain);
            teslaUserService.changeLanguage(userDo, lang);
        } else {
            log.error("Cannot write user language into cookie/db, lang split length not 2, lang={}", lang);
        }

        // 组装登录结果
        LoginUserInfoVO result = new LoginUserInfoVO();
        result.setLoginName(userDo.getLoginName());
        result.setEmpId(userDo.getEmpId());
        result.setUserName(userDo.getUsername());
        result.setNickName(userDo.getNickName());
        result.setEmail(userDo.getEmail());
        result.setBucId(Optional.ofNullable(userDo.getBucId()).orElse(0L).intValue());
        result.setBid(userDo.getBid());
        //更新用户上次登陆时间 TODO
        return buildSuccessTeslaResult(userDo);
    }

    /**
     * 统一退出登录处理
     *
     * @param request
     * @param response
     */
    @RequestMapping(value = "auth/logout", method = RequestMethod.GET)
    @ResponseBody
    public com.alibaba.tesla.common.base.TeslaResult logout(HttpServletRequest request, HttpServletResponse response, @RequestParam String appId) {
        //获取请求referer，注销之后，在此登录
        String requestReferer = request.getHeader("Referer");
        AppDO appDo = teslaAppService.getByAppId(appId);
        if (null == appDo) {
            log.error("App {} is not exit", appId);
            throw new ApplicationException(TeslaResult.FAILURE, "error.app.NotExits");
        }
        if (null == requestReferer || requestReferer.length() == 0) {
            // referer为空默认注销之后，在此再次登录跳转到应用配置的callback首页地址
            requestReferer = appDo.getIndexUrl();
            if (log.isDebugEnabled()) {
                log.debug("Referer is empty, set logout callback url is {}", requestReferer);
            }
        }
        if (!StringUtil.isEmpty(requestReferer)) {
            try {
                requestReferer = URLEncoder.encode(requestReferer, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                log.error("AAS登出回调地址URLEndode编码错误");
                ResponseUtil.writeErrorJson(response, TeslaResult.FAILURE, null, Constants.RES_MSG_SERVERERROR);
            }
        }

        // 清除登录用户session信息
        request.getSession().invalidate();
        authPolicy.getLoginServiceManager().logout(request, response, requestReferer);
        return TeslaResultBuilder.successResult();
    }
}