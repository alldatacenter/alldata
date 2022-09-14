package com.alibaba.tesla.authproxy.oauth2;

import com.alibaba.tesla.authproxy.AuthProperties;
import com.alibaba.tesla.authproxy.constants.AuthJwtConstants;
import com.alibaba.tesla.authproxy.model.UserDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class TeslaOAuth2TokenEnhancer implements TokenEnhancer {

    @Autowired
    private AuthProperties authProperties;

    @Override
    public OAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
        final Map<String, Object> additionalInfo = new HashMap<>();
        //UserDO userDo = (UserDO)authentication.getPrincipal();
        //additionalInfo.put("loginName", userDo.getLoginName());
        //if (!Constants.ENVIRONMENT_INTERNAL.equals(authProperties.getEnvironment())) {
        //    additionalInfo.put("userId", userDo.getAliyunPk());
        //} else {
        //    additionalInfo.put("userId", userDo.getBucId());
        //}
        //((DefaultOAuth2AccessToken)accessToken).setAdditionalInformation(additionalInfo);
        Map details = (Map)authentication.getUserAuthentication().getDetails();
        additionalInfo.put(AuthJwtConstants.JWT_APP_ID_CLAIM_KEY, details.get("client_id"));
        additionalInfo.put(AuthJwtConstants.JWT_APP_SECRET_CLAIM_KEY, details.get("client_secret"));
        ((DefaultOAuth2AccessToken)accessToken).setAdditionalInformation(additionalInfo);
        return accessToken;
    }
}