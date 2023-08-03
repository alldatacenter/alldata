/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seatunnel.app.adapter;

import org.apache.seatunnel.app.interceptor.AuthenticationInterceptor;
import org.apache.seatunnel.app.resolver.UserIdMethodArgumentResolver;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

import javax.annotation.Resource;

import java.util.List;
import java.util.Locale;

@Configuration
public class SeatunnelWebAdapter implements WebMvcConfigurer {

    public static final String LOCALE_LANGUAGE_COOKIE = "language";
    public static final String LOGIN_INTERCEPTOR_PATH_PATTERN = "/**/*";
    public static final String LOGIN_PATH_PATTERN = "/seatunnel/api/v1/user/login**";
    public static final String REGISTER_PATH_PATTERN = "/users/register";

    @Bean
    public AuthenticationInterceptor authenticationInterceptor() {
        return new AuthenticationInterceptor();
    }

    @Resource private UserIdMethodArgumentResolver currentUserMethodArgumentResolver;

    /**
     * Cookie
     *
     * @return local resolver
     */
    @Bean(name = "localeResolver")
    public LocaleResolver localeResolver() {
        CookieLocaleResolver localeResolver = new CookieLocaleResolver();
        localeResolver.setCookieName(LOCALE_LANGUAGE_COOKIE);
        // set default locale
        localeResolver.setDefaultLocale(Locale.US);
        // set language tag compliant
        localeResolver.setLanguageTagCompliant(false);
        return localeResolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authenticationInterceptor())
                .order(1)
                .addPathPatterns(LOGIN_INTERCEPTOR_PATH_PATTERN)
                .excludePathPatterns(
                        LOGIN_PATH_PATTERN,
                        REGISTER_PATH_PATTERN,
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/v2/**",
                        "*.html",
                        "/ui/**",
                        "/error",
                        "/swagger-ui.html**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(currentUserMethodArgumentResolver);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
        registry.addResourceHandler("/ui/**").addResourceLocations("file:ui/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("redirect:/ui/");
        registry.addViewController("/ui/").setViewName("forward:/ui/index.html");
    }
}
