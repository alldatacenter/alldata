
package com.platform.app.bean;

import java.io.Serializable;

/**
 * Created by wulinhao
 * Time  2019/9/8
 * Describe:  用户信息
 */

public class User implements Serializable {

    private Long   id;
    private String email;
    private String logo_url;
    private String username;
    private String mobi;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLogo_url() {
        return logo_url;
    }

    public void setLogo_url(String logo_url) {
        this.logo_url = logo_url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMobi() {
        return mobi;
    }

    public void setMobi(String mobi) {
        this.mobi = mobi;
    }
}
