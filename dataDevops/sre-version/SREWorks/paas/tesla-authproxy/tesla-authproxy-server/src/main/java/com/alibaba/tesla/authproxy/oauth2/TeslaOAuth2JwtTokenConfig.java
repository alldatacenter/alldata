package com.alibaba.tesla.authproxy.oauth2;

import com.alibaba.tesla.authproxy.AuthProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

/**
 * JWT Token 信息配置
 */
@Configuration
public class TeslaOAuth2JwtTokenConfig {

    @Autowired
    private AuthProperties authProperties;

    @Autowired
    private TeslaOAuth2AccessTokenConverter teslaOAuth2AccessTokenConverter;

    /**
     * JWT token 存储
     */
    @Bean
    public TokenStore tokenStore() {
        return new JwtTokenStore(accessTokenConverter());
    }

    /**
     * JWT access token 转换器
     */
    @Bean
    public JwtAccessTokenConverter accessTokenConverter() {
        JwtAccessTokenConverter accessTokenConverter = new JwtAccessTokenConverter();
        accessTokenConverter.setSigningKey(authProperties.getOauth2JwtSecret());
        accessTokenConverter.setAccessTokenConverter(teslaOAuth2AccessTokenConverter);
        return accessTokenConverter;
    }
}