package cn.datax.common.security.config;

import cn.datax.common.security.interceptor.DataServerProtectInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

public class DataSecurityInteceptorConfig implements WebMvcConfigurer {

    @Bean
    public HandlerInterceptor dataServerProtectInterceptor() {
        return new DataServerProtectInterceptor();
    }

    @Override
    @SuppressWarnings("all")
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(dataServerProtectInterceptor());
    }
}
