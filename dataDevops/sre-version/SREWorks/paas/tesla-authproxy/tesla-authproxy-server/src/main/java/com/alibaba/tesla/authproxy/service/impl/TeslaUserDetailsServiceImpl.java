package com.alibaba.tesla.authproxy.service.impl;

import com.alibaba.tesla.authproxy.AuthProperties;
import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.model.mapper.UserMapper;
import com.alibaba.tesla.authproxy.service.TeslaUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Tesla 用户详情服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service("teslaUserDetailsService")
@Slf4j
public class TeslaUserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private AuthProperties authProperties;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TeslaUserService teslaUserService;

    /**
     * 根据用户名获取对应的用户详情
     *
     * @param username 用户名 (Spring 内部使用)
     * @return 用户信息对象
     * @throws UsernameNotFoundException 没有找到时抛出
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String environment = authProperties.getEnvironment();
        UserDetails user;
        switch (environment) {
            case Constants.ENVIRONMENT_INTERNAL:
                user = teslaUserService.getUserByLoginName(username);
                break;
            default:
                user = teslaUserService.getUserByAccessKeyId(username);
                break;
        }
        if (user == null) {
            throw new UsernameNotFoundException(String.format("Cannot find username %s in %s environment",
                username, environment));
        }
        return user;
    }
}
