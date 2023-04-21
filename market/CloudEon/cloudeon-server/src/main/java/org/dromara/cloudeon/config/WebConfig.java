package org.dromara.cloudeon.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private CloudeonWebProperties webProperties;

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // 根据不同包匹配表达式，添加各自的统一前缀
        configurePathMatch(configurer, webProperties.getApi());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器，校验规则为 StpUtil.checkLogin() 登录校验。
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/apiPre/**")
                .excludePathPatterns("/apiPre/acc/doLogin")
                .excludePathPatterns("/apiPre/alert/webhook")
        ;
    }
    /**
     * API 前缀：实现指定的controller 提供的 RESTFul API 的统一前缀
     *
     * 意义：通过该前缀，避免Swagger,Actuator 意外通过Nginx暴露出来给外部，带来安全性问题
     *      这样Nginx只需配置转发到 指定统一前缀 的所有接口即可
     * @see org.springframework.util.AntPathMatcher
     * @param configurer
     * @param api
     */
    private void configurePathMatch(PathMatchConfigurer configurer, CloudeonWebProperties.Api api) {
        // 创建路径匹配类，指定以'.'分隔
        AntPathMatcher antPathMatcher = new AntPathMatcher(".");
        // 指定匹配前缀
        // 满足：类上有RestController注解 && 该类的包名匹配指定的自定义包的表达式
        configurer.addPathPrefix(api.getPrefix(), clazz -> clazz.isAnnotationPresent(RestController.class)
                && antPathMatcher.match(api.getControllerPath(), clazz.getPackage().getName()));
    }
}
