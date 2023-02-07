package cn.datax.common.core;

import cn.hutool.core.util.ObjectUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class DataUser extends User {

    private String id;
    private String nickname;
    private String dept;
    private List<DataRole> roles;
    private List<String> posts;

    public DataUser(String id, String nickname, String username, String password, boolean enabled, boolean accountNonExpired, boolean credentialsNonExpired, boolean accountNonLocked, Collection<? extends GrantedAuthority> authorities) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.id = id;
        this.nickname = nickname;
    }

    public boolean isAdmin() {
        return isAdmin(this.getUsername());
    }

    public static boolean isAdmin(String username) {
        return ObjectUtil.equal(username, "admin");
    }
}
