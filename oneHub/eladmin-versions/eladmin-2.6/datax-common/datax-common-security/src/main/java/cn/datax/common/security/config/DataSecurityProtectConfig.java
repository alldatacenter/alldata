package cn.datax.common.security.config;

import cn.datax.common.core.DataConstant;
import cn.datax.common.security.handler.DataAccessDeniedHandler;
import cn.datax.common.security.handler.DataAuthExceptionEntryPoint;
import feign.RequestInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.util.Base64Utils;

public class DataSecurityProtectConfig {

    @Bean
    @ConditionalOnMissingBean(name = "accessDeniedHandler")
    public DataAccessDeniedHandler accessDeniedHandler() {
        return new DataAccessDeniedHandler();
    }

    @Bean
    @ConditionalOnMissingBean(name = "authenticationEntryPoint")
    public DataAuthExceptionEntryPoint authenticationEntryPoint() {
        return new DataAuthExceptionEntryPoint();
    }

    @Bean
    @ConditionalOnMissingBean(value = PasswordEncoder.class)
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DataSecurityInnerAspect dataSecurityInnerAspect() {
        return new DataSecurityInnerAspect();
    }

    @Bean
    public DataSecurityInteceptorConfig dataSecurityInteceptorConfig() {
        return new DataSecurityInteceptorConfig();
    }

    @Bean
    public RequestInterceptor oauth2FeignRequestInterceptor() {
        return requestTemplate -> {
            // 请求头中添加 Gateway Token
            String gatewayToken = new String(Base64Utils.encode(DataConstant.Security.TOKENVALUE.getVal().getBytes()));
            requestTemplate.header(DataConstant.Security.TOKENHEADER.getVal(), gatewayToken);
            // 请求头中添加原请求头中的 Token
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                Object details = authentication.getDetails();
                if (details instanceof OAuth2AuthenticationDetails) {
                    String authorizationToken = ((OAuth2AuthenticationDetails) details).getTokenValue();
                    requestTemplate.header(DataConstant.Security.AUTHORIZATION.getVal(), DataConstant.Security.TOKENTYPE.getVal() + authorizationToken);
                }
            }
        };
    }
}
