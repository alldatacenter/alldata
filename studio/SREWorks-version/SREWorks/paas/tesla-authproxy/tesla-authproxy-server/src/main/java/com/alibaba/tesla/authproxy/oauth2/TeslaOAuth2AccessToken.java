package com.alibaba.tesla.authproxy.oauth2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.oauth2.common.ExpiringOAuth2RefreshToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Tesla OAuth2 Access Token
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeslaOAuth2AccessToken {

    /**
     * Token 类型
     */
    private String tokenType;

    /**
     * Token
     */
    private String token;

    /**
     * Token 过期时间
     */
    private Long tokenExpiration;

    /**
     * Token 是否已经过期
     */
    private Boolean tokenIsExpired;

    /**
     * Token 会在多长时间内过期
     */
    private Long tokenExpiresIn;

    /**
     * 刷新 Token
     */
    private String refreshToken;

    /**
     * 刷新 Token 过期时间
     */
    private Long refreshTokenExpiration;

    /**
     * 额外信息
     */
    private Map<String, Object> extra;

    /**
     * 根据原生的 OAuth2AccessToken 获取一个新的适合输出的 OAuth2 Access Token
     *
     * @param rawToken 原始 OAuth2 Token
     * @return TeslaOAuth2AccessToken
     */
    public static TeslaOAuth2AccessToken newInstance(OAuth2AccessToken rawToken) {
        long nowMillis = System.currentTimeMillis();
        Date nowDate = new Date();
        ExpiringOAuth2RefreshToken refreshToken = (ExpiringOAuth2RefreshToken)rawToken.getRefreshToken();
        TeslaOAuth2AccessToken instance = new TeslaOAuth2AccessToken();
        Date tokenExpiration = rawToken.getExpiration();
        if (tokenExpiration != null) {
            instance.setTokenExpiration(tokenExpiration.getTime() / 1000);
        }
        Date refreshTokenExpiration = refreshToken.getExpiration();
        if (refreshTokenExpiration != null) {
            instance.setRefreshTokenExpiration(refreshTokenExpiration.getTime() / 1000);
        }
        instance.setTokenType(rawToken.getTokenType());
        instance.setToken(rawToken.getValue());
        instance.setRefreshToken(refreshToken.getValue());
        instance.setTokenIsExpired(tokenExpiration != null && tokenExpiration.before(nowDate));
        instance.setTokenExpiresIn(
            tokenExpiration != null ? Long.valueOf((tokenExpiration.getTime() - nowMillis) / 1000L).intValue() : 0L);
        if (rawToken.getAdditionalInformation() != null) {
            instance.setExtra(rawToken.getAdditionalInformation());
        } else {
            instance.setExtra(new HashMap<>());
        }
        return instance;
    }
}
