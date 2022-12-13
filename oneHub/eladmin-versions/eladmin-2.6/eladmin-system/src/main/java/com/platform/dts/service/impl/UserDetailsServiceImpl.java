package com.platform.dts.service.impl;

import com.platform.dts.entity.JobUser;
import com.platform.dts.entity.JwtUser;
import com.platform.dts.mapper.JobUserMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * UserDetailsServiceImpl
 * @author AllDataDC
 * @since 2022/11/15
 * @version v2.1.1
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Resource
    private JobUserMapper jobUserMapper;

    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        JobUser user = jobUserMapper.loadByUserName(s);
        return new JwtUser(user);
    }

}
