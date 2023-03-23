package cn.datax.common.core;

import cn.hutool.core.util.ObjectUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.List;

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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getDept() {
        return dept;
    }

    public void setDept(String dept) {
        this.dept = dept;
    }

    public List<DataRole> getRoles() {
        return roles;
    }

    public void setRoles(List<DataRole> roles) {
        this.roles = roles;
    }

    public List<String> getPosts() {
        return posts;
    }

    public void setPosts(List<String> posts) {
        this.posts = posts;
    }

    @Override
    public String toString() {
        return "DataUser{" +
                "id='" + id + '\'' +
                ", nickname='" + nickname + '\'' +
                ", dept='" + dept + '\'' +
                ", roles=" + roles +
                ", posts=" + posts +
                '}';
    }
}
