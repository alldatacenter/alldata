package cn.datax.service.data.metadata.config;

import cn.datax.common.security.handler.DataAccessDeniedHandler;
import cn.datax.common.security.handler.DataAuthExceptionEntryPoint;
import cn.datax.common.security.utils.DataRedisTokenServices;
import cn.datax.common.security.utils.RedisTokenStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.TokenStore;

@Configuration
@EnableResourceServer
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class DataResourceServerConfig extends ResourceServerConfigurerAdapter {

    @Autowired
    private DataAccessDeniedHandler accessDeniedHandler;

    @Autowired
    private DataAuthExceptionEntryPoint exceptionEntryPoint;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Bean
    public TokenStore redisTokenStore() {
        return new RedisTokenStore(redisConnectionFactory);
    }

    @Override
    public void configure(ResourceServerSecurityConfigurer resources) {
        DataRedisTokenServices dataTokenServices = new DataRedisTokenServices();
        dataTokenServices.setTokenStore(redisTokenStore());

        resources
                .tokenStore(redisTokenStore())
                .tokenServices(dataTokenServices)
                .authenticationEntryPoint(exceptionEntryPoint)
                .accessDeniedHandler(accessDeniedHandler);
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        //允许使用iframe 嵌套，避免swagger-ui 不被加载的问题
        http.headers().frameOptions().disable();
        http.authorizeRequests()
                .antMatchers(
                        "/actuator/**",
                        "/v2/api-docs/**",
                        "/swagger-ui.html",
                        "/doc.html",
                        "/swagger-resources/**",
                        "/webjars/**",
                        // feign 内部调用不用授权
                        "/inner/**"
                ).permitAll()
                .anyRequest().authenticated()
                .and().csrf().disable();
    }
}
