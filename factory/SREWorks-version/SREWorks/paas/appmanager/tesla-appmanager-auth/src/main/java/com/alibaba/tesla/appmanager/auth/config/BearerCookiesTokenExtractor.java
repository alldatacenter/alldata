package com.alibaba.tesla.appmanager.auth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.authentication.BearerTokenExtractor;
import org.springframework.security.oauth2.provider.authentication.TokenExtractor;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * 扩展 Token Extractor
 *
 * 用于解析 Header 中的 Token 和 Cookie 中的 Token，两者都生效
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class BearerCookiesTokenExtractor implements TokenExtractor {

    private final BearerTokenExtractor tokenExtractor = new BearerTokenExtractor();

    @Override
    public Authentication extract(final HttpServletRequest request) {
        Authentication authentication = tokenExtractor.extract(request);
        if (authentication == null && request.getCookies() != null && request.getCookies().length > 0) {
            Optional<PreAuthenticatedAuthenticationToken> authenticationMap = Arrays.stream(request.getCookies())
                .filter(isValidTokenCookie())
                .findFirst()
                .map(cookie -> new PreAuthenticatedAuthenticationToken(cookie.getValue(), EMPTY));
            if (authenticationMap.isPresent()) {
                authentication = authenticationMap.orElseGet(null);
            }
        }
        return authentication;
    }

    private Predicate<Cookie> isValidTokenCookie() {
        return cookie -> cookie.getName().equals(AuthConstant.ACCESS_TOKEN_COOKIE_NAME);
    }
}