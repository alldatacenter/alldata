package com.alibaba.tesla.authproxy.config;

import com.alibaba.tesla.authproxy.interceptor.AuthProxyInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
//import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

/**
 * 拦截器配置
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Configuration
public class WebConfig extends WebMvcConfigurationSupport {

    @Autowired
    private AuthProxyInterceptor authProxyInterceptor;
    //
    //@Autowired
    //private LocaleChangeInterceptor localeChangeInterceptor;



    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 权限拦截器
        registry.addInterceptor(authProxyInterceptor).addPathPatterns("/**")
            .excludePathPatterns("/auth/private/account/login/**")
            .excludePathPatterns("/auth/user/lang")
            .excludePathPatterns("/auth/user/lang/**")
            .excludePathPatterns("/auth/private/gateway/testsend")
            .excludePathPatterns("/permission/gateway")
            .excludePathPatterns("/permission/import")
            .excludePathPatterns("/permission/meta/list")
            .excludePathPatterns("/permissionres/list")
            .excludePathPatterns("/service/meta/list")
            .excludePathPatterns("/permission/checkStrict")
            .excludePathPatterns("/auth/private/thirdparty/**")
            .excludePathPatterns("/profile")
            .excludePathPatterns("/roles/**")
            .excludePathPatterns("/users/**")
            .excludePathPatterns("/system/**")
            .excludePathPatterns("/permissions/**")
            .excludePathPatterns("/switchView/**")
            .excludePathPatterns("/swagger-resources")
            .excludePathPatterns("/oauth2/redirect");
        //
        //// 多语言拦截器
        //registry.addInterceptor(localeChangeInterceptor).addPathPatterns("/**");

        // 指标拦截器
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }
}