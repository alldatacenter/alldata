package com.alibaba.tesla.appmanager.auth.config;

import com.alibaba.tesla.appmanager.autoconfig.AuthProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.jwt.crypto.sign.MacSigner;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.*;

@Configuration
@EnableResourceServer
@Slf4j
public class Oauth2ResourceServerConfig extends ResourceServerConfigurerAdapter {

    private static final String RESOURCE_ID = "appmanager";

    // 兼容集团内部 authproxy 的部分配置
    private static final String COMPATIBLE_EMP_ID = "emp_id";
    private static final String COMPATIBLE_SCOPE = "all";
    private static final String COMPATIBLE_GRANT_TYPE = "password";
    private static final String COMPATIBLE_AUTHORITY = "authority";

    @Autowired
    private AuthProperties authProperties;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests().anyRequest().authenticated();
    }

    @Override
    public void configure(ResourceServerSecurityConfigurer resources) {
        resources.resourceId(RESOURCE_ID);
        resources.tokenStore(createTokenStore());
        resources.tokenExtractor(new BearerCookiesTokenExtractor());
    }

    private TokenStore createTokenStore() {
        JwtAccessTokenConverter tokenConverter = new CustomJwtAccessTokenConverter();
        tokenConverter.setVerifier(new MacSigner(authProperties.getJwtSecretKey()));
        return new JwtTokenStore(tokenConverter);
    }

    /**
     * 兼容内部 authproxy 的 JWT Token 和 appmanager 自行下发的 JWT Token 两种类型的不同格式
     *
     * @author yaoxing.gyx@alibaba-inc.com
     */
    public class CustomJwtAccessTokenConverter extends JwtAccessTokenConverter {

        /**
         * 后续切换到独立 RSA JWT 时可以在此处分情况 Decode
         *
         * @param token JWT Token
         */
        @Override
        protected Map<String, Object> decode(String token) {
            return super.decode(token);
        }

        @Override
        public OAuth2AccessToken extractAccessToken(String value, Map<String, ?> map) {
            // appmanager 自行下发的 JWT Token 不包含 emp_id
            if (!map.containsKey(COMPATIBLE_EMP_ID)) {
                return super.extractAccessToken(value, map);
            }

            DefaultOAuth2AccessToken token = new DefaultOAuth2AccessToken(value);
            Map<String, Object> info = new HashMap<>(map);
            info.remove(EXP);
            if (map.containsKey(EXP)) {
                token.setExpiration(new Date((Long) map.get(EXP) * 1000L));
            }
            token.setScope(Collections.singleton(COMPATIBLE_SCOPE));
            token.setAdditionalInformation(info);
            return token;
        }

        @Override
        public OAuth2Authentication extractAuthentication(Map<String, ?> map) {
            // appmanager 自行下发的 JWT Token 不包含 emp_id
            if (!map.containsKey(COMPATIBLE_EMP_ID)) {
                return super.extractAuthentication(map);
            }

            Map<String, String> parameters = new HashMap<>();
            parameters.put(GRANT_TYPE, COMPATIBLE_GRANT_TYPE);
            Set<String> scope = Collections.singleton(COMPATIBLE_SCOPE);
            UserDetails userDetail = userDetailsService.loadUserByUsername(authProperties.getSuperAccessId());
            Authentication user = new UsernamePasswordAuthenticationToken(userDetail, "N/A",
                userDetail.getAuthorities());
            String clientId = String.valueOf(map.get(COMPATIBLE_EMP_ID));
            Set<String> resourceIds = Collections.singleton(RESOURCE_ID);
            Collection<? extends GrantedAuthority> authorities = AuthorityUtils
                .createAuthorityList(COMPATIBLE_AUTHORITY);
            OAuth2Request request = new OAuth2Request(parameters, clientId, authorities, true, scope,
                resourceIds, null, null, null);
            return new OAuth2Authentication(request, user);
        }
    }


    private RequestMatcher withCookieToken() {
        return request -> request.getCookies() != null && Arrays.stream(request.getCookies())
            .anyMatch(cookie -> cookie.getName()
                .equals(AuthConstant.ACCESS_TOKEN_COOKIE_NAME));
    }
}