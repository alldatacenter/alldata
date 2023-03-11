package com.hw.lineage.server.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hw.lineage.server.application.dto.basic.RootDTO;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @description: UserDTO
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class UserDTO extends RootDTO implements UserDetails {

    private Long userId;

    private String username;

    @JsonIgnore
    private String password;

    private Boolean locked;

    private List<RoleDTO> roleList;

    private List<PermissionDTO> permissionList;

    public UserDTO() {
        this.permissionList = new ArrayList<>();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return permissionList.stream()
                .map(e -> new SimpleGrantedAuthority(e.getPermissionCode()))
                .collect(Collectors.toSet());
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
