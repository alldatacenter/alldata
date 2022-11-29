package cn.datax.common.security.utils;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.*;

import java.util.Map;

@Slf4j
public class DataRedisTokenServices implements ResourceServerTokenServices {
    @Setter
    private TokenStore tokenStore;

    @Override
    public OAuth2Authentication loadAuthentication(String accessToken) throws AuthenticationException, InvalidTokenException {
        OAuth2Authentication authentication = tokenStore.readAuthentication(accessToken);
        OAuth2AccessToken token = readAccessToken(accessToken);
        if(null == authentication || null == token){
            throw new InvalidTokenException(accessToken);
        }
        DefaultAccessTokenConverter accessTokenConverter = new DefaultAccessTokenConverter();
        UserAuthenticationConverter userTokenConverter = new DataUserAuthenticationConverter();
        accessTokenConverter.setUserTokenConverter(userTokenConverter);
        Map<String, ?> map = accessTokenConverter.convertAccessToken(token, authentication);
        if (map.containsKey("error")) {
            if (this.log.isDebugEnabled()) {
                this.log.debug("check_token returned error: " + map.get("error"));
            }
            throw new InvalidTokenException(accessToken);
        } else {
            return accessTokenConverter.extractAuthentication(map);
        }
    }

    @Override
    public OAuth2AccessToken readAccessToken(String accessToken) {
        return tokenStore.readAccessToken(accessToken);
    }
}
