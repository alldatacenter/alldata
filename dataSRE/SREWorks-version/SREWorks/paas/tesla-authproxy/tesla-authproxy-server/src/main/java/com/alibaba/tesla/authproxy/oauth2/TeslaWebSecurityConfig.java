package com.alibaba.tesla.authproxy.oauth2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;

/**
 * Web 安全配置
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class TeslaWebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    @Qualifier("teslaUserDetailsService")
    private UserDetailsService userDetailsService;

    @Autowired
    public void configureAuthentication(AuthenticationManagerBuilder authenticationManagerBuilder) throws Exception {
        authenticationManagerBuilder
            .userDetailsService(this.userDetailsService)
            .passwordEncoder(NoOpPasswordEncoder.getInstance());
    }

    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED).and()
            .authorizeRequests()
            //.antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            // 允许对于网站静态资源的无授权访问
            //.antMatchers(HttpMethod.GET, "/", "/favicon.ico", "/status.taobao").permitAll()
            // 对于获取token的rest api要允许匿名访问
            //.antMatchers("/auth/**").permitAll()
            // 除上面外的所有请求全部需要鉴权认证
            //.anyRequest().authenticated();
            .anyRequest().permitAll();
        httpSecurity.headers().cacheControl().disable();
    }
}