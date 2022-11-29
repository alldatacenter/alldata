package cn.datax.auth.config;

import cn.datax.auth.service.DataUserDetailService;
import cn.datax.auth.translator.DataWebResponseExceptionTranslator;
import cn.datax.common.core.DataConstant;
import cn.datax.common.core.DataUser;
import cn.datax.common.security.handler.DataAccessDeniedHandler;
import cn.datax.common.security.handler.DataAuthExceptionEntryPoint;
import cn.datax.common.security.utils.RedisTokenStore;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService;
import org.springframework.security.oauth2.provider.token.*;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableAuthorizationServer
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private DataUserDetailService userDetailService;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private DataWebResponseExceptionTranslator exceptionTranslator;

    @Autowired
    private DataAccessDeniedHandler accessDeniedHandler;

    @Autowired
    private DataAuthExceptionEntryPoint exceptionEntryPoint;

    /**
     * 配置客户端详情服务
     * @param clients
     * @throws Exception
     */
    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        // 默认数据库的配置
        // datax:123456
        // normal-app:normal-app
        // trusted-app:trusted-app
        clients.jdbc(dataSource).clients(clientDetails());
    }

    /**
     * 用来配置令牌端点(Token Endpoint)的安全约束.
     * @param security
     * @throws Exception
     */
    @Override
    public void configure(AuthorizationServerSecurityConfigurer security) {
        security.tokenKeyAccess("permitAll()")
                .checkTokenAccess("isAuthenticated()")
                .allowFormAuthenticationForClients()
                .authenticationEntryPoint(exceptionEntryPoint)
                .accessDeniedHandler(accessDeniedHandler);
    }

    /**
     * 用来配置授权（authorization）以及令牌（token）的访问端点和令牌服务(token services)
     * @param endpoints
     * @throws Exception
     */
    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) {
        endpoints.tokenStore(tokenStore())
                .tokenServices(tokenServices())
                .userDetailsService(userDetailService)
                .authenticationManager(authenticationManager)
                .exceptionTranslator(exceptionTranslator);
    }

    @Bean
    public ClientDetailsService clientDetails() {
        return new JdbcClientDetailsService(dataSource);
    }

    @Bean
    public TokenStore tokenStore(){
        return new RedisTokenStore(redisConnectionFactory);
    }

    @Bean
    public DefaultTokenServices tokenServices(){
        DefaultTokenServices tokenServices = new DefaultTokenServices();
        tokenServices.setTokenStore(tokenStore());
        tokenServices.setSupportRefreshToken(true);
        tokenServices.setClientDetailsService(clientDetails());
        tokenServices.setTokenEnhancer(tokenEnhancer());
//        // token有效期自定义设置，默认12小时 设置为24小时86400
//        tokenServices.setAccessTokenValiditySeconds(60 * 60 * 24 * 1);
//        // refresh_token默认30天 设置为7天604800
//        tokenServices.setRefreshTokenValiditySeconds(60 * 60 * 24 * 7);
        return tokenServices;
    }

    @Bean
    public TokenEnhancer tokenEnhancer() {
        return (accessToken, authentication) -> {
            final Map<String, Object> additionalInfo = new HashMap<>();
            DataUser user = (DataUser) authentication.getUserAuthentication().getPrincipal();
            additionalInfo.put(DataConstant.UserAdditionalInfo.LICENSE.getKey(), DataConstant.UserAdditionalInfo.LICENSE.getVal());
            additionalInfo.put(DataConstant.UserAdditionalInfo.USERID.getKey(), user.getId());
            additionalInfo.put(DataConstant.UserAdditionalInfo.USERNAME.getKey(), user.getUsername());
            additionalInfo.put(DataConstant.UserAdditionalInfo.NICKNAME.getKey(), user.getNickname());

            if (StrUtil.isNotBlank(user.getDept())){
                additionalInfo.put(DataConstant.UserAdditionalInfo.DEPT.getKey(), user.getDept());
            }
            if (CollUtil.isNotEmpty(user.getRoles())){
                additionalInfo.put(DataConstant.UserAdditionalInfo.ROLE.getKey(), user.getRoles());
            }
            if (CollUtil.isNotEmpty(user.getPosts())){
                additionalInfo.put(DataConstant.UserAdditionalInfo.POST.getKey(), user.getPosts());
            }
            ((DefaultOAuth2AccessToken) accessToken).setAdditionalInformation(additionalInfo);
            return accessToken;
        };
    }
}
