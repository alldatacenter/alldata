package com.alibaba.tesla.appmanager.auth.controller;

import com.alibaba.tesla.web.controller.BaseController;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.security.oauth2.provider.token.TokenStore;

import java.util.Map;

/**
 * AppManager Base Controller
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class AppManagerBaseController extends BaseController {

    private static final String UNKNOWN_USER = "UNKNOWN";
    private static final String USERNAME = "user_name";
    private static final String COMPATIBLE_EMP_ID = "emp_id";

    @Autowired
    private TokenStore tokenStore;

    /**
     * 获取当前的操作用户
     */
    protected String getOperator(OAuth2Authentication auth) {
        if (auth == null) {
            return UNKNOWN_USER;
        }
        OAuth2AuthenticationDetails details = (OAuth2AuthenticationDetails) auth.getDetails();
        String tokenValue = details.getTokenValue();
        Map<String, Object> additionalInformation = tokenStore.readAccessToken(tokenValue).getAdditionalInformation();
        if (additionalInformation.containsKey(COMPATIBLE_EMP_ID)) {
            return String.valueOf(additionalInformation.get(COMPATIBLE_EMP_ID));
        } else {
            return String.valueOf(additionalInformation.getOrDefault(USERNAME, "UNKNOWN"));
        }
    }

    /**
     * 获取当前的操作用户
     */
    protected String getOperator(OAuth2Authentication auth, String empId) {
        String operator = getOperator(auth);
        if (UNKNOWN_USER.equals(operator) && StringUtils.isNotEmpty(empId)) {
            return empId;
        }
        return operator;
    }
}
