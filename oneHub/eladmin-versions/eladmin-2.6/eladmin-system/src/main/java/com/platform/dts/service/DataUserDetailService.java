package com.platform.dts.service;

import com.platform.exception.BadRequestException;
import com.platform.exception.EntityNotFoundException;
import com.platform.modules.security.service.UserCacheManager;
import com.platform.modules.security.service.dto.JwtUserDto;
import com.platform.modules.system.service.DataService;
import com.platform.modules.system.service.RoleService;
import com.platform.modules.system.service.UserService;
import com.platform.modules.system.service.dto.UserLoginDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Slf4j
@Service
public class DataUserDetailService implements UserDetailsService {
    @Autowired
    private UserService userService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private DataService dataService;
    @Autowired
    private UserCacheManager userCacheManager;

    @Override
    public UserDetails loadUserByUsername(String username) {
        JwtUserDto jwtUserDto = userCacheManager.getUserCache(username);
        if(jwtUserDto == null){
            UserLoginDto user;
            try {
                user = userService.getLoginData(username);
            } catch (EntityNotFoundException e) {
                // SpringSecurity会自动转换UsernameNotFoundException为BadCredentialsException
                throw new UsernameNotFoundException(username, e);
            }
            if (user == null) {
                throw new UsernameNotFoundException("");
            } else {
                if (!user.getEnabled()) {
                    throw new BadRequestException("账号未激活！");
                }
                jwtUserDto = new JwtUserDto(
                        user,
                        dataService.getDeptIds(user),
                        roleService.mapToGrantedAuthorities(user)
                );
                // 添加缓存数据
                userCacheManager.addUserCache(username, jwtUserDto);
            }
        }
        UserDetails userDetails = toUserDetails(jwtUserDto);
        return userDetails;
    }

    /**
     * 修改认证实体UserDetails
     * @param jwtUserDto
     * @return
     */
    public UserDetails toUserDetails(JwtUserDto jwtUserDto) {
        return new UserDetails() {
            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return jwtUserDto.getAuthorities();
            }

            @Override
            public String getPassword() {
                return jwtUserDto.getPassword();
            }

            @Override
            public String getUsername() {
                return jwtUserDto.getUsername();
            }

            @Override
            public boolean isAccountNonExpired() {
                return jwtUserDto.isAccountNonExpired();
            }

            @Override
            public boolean isAccountNonLocked() {
                return jwtUserDto.isAccountNonLocked();
            }

            @Override
            public boolean isCredentialsNonExpired() {
                return jwtUserDto.isCredentialsNonExpired();
            }

            @Override
            public boolean isEnabled() {
                return jwtUserDto.isEnabled();
            }
        };
    }
}
