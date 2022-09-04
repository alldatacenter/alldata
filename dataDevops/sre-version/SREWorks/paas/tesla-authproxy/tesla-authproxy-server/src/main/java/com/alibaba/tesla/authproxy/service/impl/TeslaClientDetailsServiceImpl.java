package com.alibaba.tesla.authproxy.service.impl;

import com.alibaba.tesla.authproxy.model.AppExtDO;
import com.alibaba.tesla.authproxy.model.mapper.AppExtMapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Service("teslaClientDetailsService")
@Slf4j
public class TeslaClientDetailsServiceImpl implements ClientDetailsService {

    /**
     * Access Token 有效期，秒
     */
    private static final Integer ACCESS_TOKEN_VALIDITY_SECONDS = 7200;

    /**
     * Refresh Token 有效期，秒
     */
    private static final Integer REFRESH_TOKEN_VALIDITY_SECONDS = 2592000;

    @Autowired
    private AppExtMapper appExtMapper;

    @Override
    public ClientDetails loadClientByClientId(String clientId) throws ClientRegistrationException {
        AppExtDO app = appExtMapper.getByName(clientId);
        if (app == null) {
            throw new ClientRegistrationException("Cannot find client with clientId " + clientId);
        }

        BaseClientDetails details = new BaseClientDetails();
        details.setClientId(clientId);
        details.setClientSecret(app.getExtAppKey());
        details.setAuthorizedGrantTypes(Arrays.asList("password", "refresh_token"));
        details.setScope(Collections.singletonList("all"));
        details.setAccessTokenValiditySeconds(ACCESS_TOKEN_VALIDITY_SECONDS);
        details.setRefreshTokenValiditySeconds(REFRESH_TOKEN_VALIDITY_SECONDS);
        Set<GrantedAuthority> authorities = new HashSet<>();
        // TODO: 增加 client 的 authorities
        authorities.add(new SimpleGrantedAuthority("NONE"));
        details.setAuthorities(authorities);
        return details;
    }
}
