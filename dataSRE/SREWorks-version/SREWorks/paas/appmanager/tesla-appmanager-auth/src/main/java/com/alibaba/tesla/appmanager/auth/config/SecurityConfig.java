package com.alibaba.tesla.appmanager.auth.config;

import com.alibaba.tesla.appmanager.autoconfig.AuthProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private AuthProperties authProperties;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Override
    protected UserDetailsService userDetailsService() {
        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
        manager.createUser(User.withUsername(authProperties.getSuperAccessId())
                .password(passwordEncoder().encode(authProperties.getSuperAccessSecret()))
                .authorities("ROLE_ADMIN").build());
        return manager;
    }

    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        if (authProperties.getEnableAuth()) {
            web.ignoring()
                    .antMatchers("/status.taobao")
                    .antMatchers("/actuator/**")
                    .antMatchers("/traits**")
                    .antMatchers("/flow-manager/**")
                    .antMatchers("/traits/**")
                    .antMatchers(HttpMethod.POST, "/apps")
                    .antMatchers("/definition-schemas**")
                    .antMatchers(HttpMethod.GET, "/realtime/**");
        } else {
            web.ignoring().antMatchers("/**");
        }
    }
}
