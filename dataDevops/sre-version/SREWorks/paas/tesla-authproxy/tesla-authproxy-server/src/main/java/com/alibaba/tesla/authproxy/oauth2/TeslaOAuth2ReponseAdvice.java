package com.alibaba.tesla.authproxy.oauth2;

import com.alibaba.tesla.common.base.util.TeslaGsonUtil;
import com.alibaba.tesla.common.utils.TeslaResultBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice
@Slf4j
public class TeslaOAuth2ReponseAdvice implements ResponseBodyAdvice<Object> {

    private static final String LOG_PRE = "[" + TeslaOAuth2ReponseAdvice.class.getSimpleName() + "] ";

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (body instanceof OAuth2AccessToken) {
            try {
                return TeslaResultBuilder.successResult(TeslaOAuth2AccessToken.newInstance((OAuth2AccessToken)body));
            } catch (Exception e) {
                log.error(LOG_PRE + "Build tesla oauth2 access token request failed, body={}, exception={}",
                    TeslaGsonUtil.toJson(body), ExceptionUtils.getStackTrace(e));
                return TeslaResultBuilder.errorResult(e);
            }
        }
        return body;
    }
}
