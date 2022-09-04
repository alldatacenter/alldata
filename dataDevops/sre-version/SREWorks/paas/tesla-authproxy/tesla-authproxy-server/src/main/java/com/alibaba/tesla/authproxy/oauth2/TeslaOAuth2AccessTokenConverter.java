package com.alibaba.tesla.authproxy.oauth2;

import com.alibaba.tesla.authproxy.constants.AuthJwtConstants;
import com.alibaba.tesla.authproxy.model.UserDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.DefaultUserAuthenticationConverter;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Slf4j
public class TeslaOAuth2AccessTokenConverter extends DefaultAccessTokenConverter {

    public TeslaOAuth2AccessTokenConverter() {
        super.setUserTokenConverter(new TeslaUserAuthenticationConverter());
    }

    private class TeslaUserAuthenticationConverter extends DefaultUserAuthenticationConverter {

        @Override
        public Map<String, ?> convertUserAuthentication(Authentication authentication) {
            LinkedHashMap<String, Object> response = new LinkedHashMap<>();
            UserDO userDo = (UserDO)authentication.getPrincipal();
            response.put(AuthJwtConstants.JWT_LOGIN_NAME_CLAIM_KEY, userDo.getLoginName());
            response.put(AuthJwtConstants.JWT_USER_ID_CLAIM_KEY, userDo.getUserId());
            response.put(AuthJwtConstants.JWT_BUC_ID_CLAIM_KEY, userDo.getBucId());
            response.put(AuthJwtConstants.JWT_ALIYUN_PK_CLAIM_KEY, userDo.getAliyunPk());
            response.put(AuthJwtConstants.JWT_NICKNAME_CLAIM_KEY, userDo.getNickName());
            response.put(AuthJwtConstants.JWT_EMP_ID_CLAIM_KEY, userDo.getEmpId());
            response.put(AuthJwtConstants.JWT_EMAIL_CLAIM_KEY, userDo.getEmail());
            return response;
        }
    }
}
