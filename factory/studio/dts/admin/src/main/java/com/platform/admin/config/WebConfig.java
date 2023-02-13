package com.platform.admin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 *
 * @author AllDataDC
 * @Date: 2022/9/16 11:14
 * @Description: 前端静态资源访问
 **/
@Configuration
public class WebConfig implements WebMvcConfigurer {


    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/index.html").addResourceLocations("classpath:/static/index.html");
        registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/static/");
    }
}
