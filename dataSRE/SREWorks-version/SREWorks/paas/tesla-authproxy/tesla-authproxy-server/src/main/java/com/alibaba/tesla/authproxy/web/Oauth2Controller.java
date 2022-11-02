package com.alibaba.tesla.authproxy.web;

import com.alibaba.tesla.authproxy.AuthProperties;
import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.model.vo.AliyunAccessTokenVO;
import com.alibaba.tesla.authproxy.util.AliyunOauth2Util;
import com.alibaba.tesla.authproxy.util.CookieUtil;
import com.alibaba.tesla.common.base.util.TeslaGsonUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * OAuth2 相关的 Controller
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Controller
@RequestMapping("oauth2")
@Slf4j
public class Oauth2Controller {

    @Autowired
    private AliyunOauth2Util oauth2Util;

    @Autowired
    private AuthProperties authProperties;

    @RequestMapping(value = "/redirect", method = RequestMethod.GET)
    public void initNew(HttpServletRequest request,
        HttpServletResponse response,
        @RequestParam("code") String code,
        @RequestParam("state") String state) throws IOException {
        JsonObject stateJson = new JsonParser().parse(state).getAsJsonObject();
        AliyunAccessTokenVO token = oauth2Util.getAccessToken(code);
        log.info("Get token from aliyun, code={}, state={}, token={}", code, state, TeslaGsonUtil.toJson(token));
        String topDomain = CookieUtil.getCookieDomain(request, authProperties.getCookieDomain());
        String expiresAt = String.valueOf(System.currentTimeMillis() / Constants.MILLS_SECS
            + Long.parseLong(token.getExpiresIn()));
        CookieUtil.setCookie(response, Constants.COOKIE_ALIYUN_ACCESS_TOKEN, token.getAccessToken(), 0, topDomain);
        CookieUtil.setCookie(response, Constants.COOKIE_ALIYUN_REFRESH_TOKEN, token.getRefreshToken(), 0, topDomain);
        CookieUtil.setCookie(response, Constants.COOKIE_ALIYUN_EXPIRES_AT, expiresAt, 0, topDomain);
        //response.sendRedirect(SecurityUtil.getSafeUrl(stateJson.get("callback").getAsString()));
        response.sendRedirect(stateJson.get("callback").getAsString());
    }
}
