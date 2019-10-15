package com.platform.app.data.dao;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

/**
 * <p>
 *
 * @author :wulinhao
 * @date : 2019/08/23
 * desc    : 用户 数据库
 * </pre>
 */

@Entity
public class User {

    @Id(autoincrement = true)
    private Long userId;

    private String phone;

    private String pwd;

    private int sex;

    private String nickName;

    private long regetTime;

    @Generated(hash = 1109236427)
    public User(Long userId, String phone, String pwd, int sex, String nickName,
                long regetTime) {
        this.userId = userId;
        this.phone = phone;
        this.pwd = pwd;
        this.sex = sex;
        this.nickName = nickName;
        this.regetTime = regetTime;
    }

    @Generated(hash = 586692638)
    public User() {
    }

    public Long getUserId() {
        return this.userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPhone() {
        return this.phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPwd() {
        return this.pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public int getSex() {
        return this.sex;
    }

    public void setSex(int sex) {
        this.sex = sex;
    }

    public String getNickName() {
        return this.nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public long getRegetTime() {
        return this.regetTime;
    }

    public void setRegetTime(long regetTime) {
        this.regetTime = regetTime;
    }


}
