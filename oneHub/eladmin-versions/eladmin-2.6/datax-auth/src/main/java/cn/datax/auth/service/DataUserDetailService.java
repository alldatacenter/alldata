package cn.datax.auth.service;

import cn.datax.common.core.DataConstant;
import cn.datax.common.core.DataRole;
import cn.datax.common.core.DataUser;
import cn.datax.service.system.api.feign.UserServiceFeign;
import cn.datax.service.system.api.vo.*;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DataUserDetailService implements UserDetailsService {

    @Autowired
    private UserServiceFeign userServiceFeign;

    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        // 远程获取用户
        UserInfo userInfo = userServiceFeign.loginByUsername(s);
        if(userInfo == null){
            throw new UsernameNotFoundException(StrUtil.format("{}用户不存在", s));
        }
        // 可用性 :true:可用 false:不可用
        boolean enabled = true;
        // 过期性 :true:没过期 false:过期
        boolean accountNonExpired = true;
        // 有效性 :true:凭证有效 false:凭证无效
        boolean credentialsNonExpired = true;
        // 锁定性 :true:未锁定 false:已锁定
        boolean accountNonLocked = true;
        Set<String> authsSet = new HashSet<>();
        if (ArrayUtil.isNotEmpty(userInfo.getPerms())) {
            authsSet.addAll(Arrays.asList(userInfo.getPerms()));
        }
        UserVo userVo = userInfo.getUserVo();
        List<RoleVo> roles = userVo.getRoles();
        if (CollUtil.isNotEmpty(roles)) {
            roles.stream()
                    .filter(roleVo -> StrUtil.isNotBlank(roleVo.getRoleCode()))
                    .forEach(roleVo -> authsSet.add(DataConstant.Security.ROLEPREFIX.getVal() + roleVo.getRoleCode()));
        }
        if(CollUtil.isEmpty(authsSet)){
            authsSet.add(DataConstant.Security.ROLEPREFIX.getVal() + "VISITOR");
        }
        Collection<? extends GrantedAuthority> authorities
                = AuthorityUtils.createAuthorityList(authsSet.toArray(new String[0]));
        DataUser user = new DataUser(userVo.getId(), userVo.getNickname(), userVo.getUsername(), userVo.getPassword(),
                enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        if(StrUtil.isNotBlank(userVo.getDeptId())){
            user.setDept(userVo.getDeptId());
        }
        if (CollUtil.isNotEmpty(userVo.getPosts())) {
            user.setPosts(userVo.getPosts().stream().map(PostVo::getId).collect(Collectors.toList()));
        }
        if (CollUtil.isNotEmpty(userVo.getRoles())) {
            user.setRoles(userVo.getRoles().stream().map(roleVo -> {
                DataRole dataRole = new DataRole();
                dataRole.setId(roleVo.getId());
                dataRole.setDataScope(roleVo.getDataScope());
                return dataRole;
            }).collect(Collectors.toList()));
        }
        return user;
    }
}
