package com.platform.app.data.dao;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

/**
 * <p>
 *
 * @author :wulinhao
 * @date : 2019/08/23
 * desc    : 用户地址
 * </pre>
 */

@Entity
public class Address{

    @Id(autoincrement = true)
    private Long addressId;

    private Long userId;

    private String name;

    private String phone;

    private boolean isDefaultAddress;

    private String bigAddress;

    private String smallAddress;

    private String address;

    @Generated(hash = 1449850013)
    public Address(Long addressId, Long userId, String name, String phone,
            boolean isDefaultAddress, String bigAddress, String smallAddress,
            String address) {
        this.addressId = addressId;
        this.userId = userId;
        this.name = name;
        this.phone = phone;
        this.isDefaultAddress = isDefaultAddress;
        this.bigAddress = bigAddress;
        this.smallAddress = smallAddress;
        this.address = address;
    }

    @Generated(hash = 388317431)
    public Address() {
    }

    public Long getAddressId() {
        return this.addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }

    public Long getUserId() {
        return this.userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return this.phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean getIsDefaultAddress() {
        return this.isDefaultAddress;
    }

    public void setIsDefaultAddress(boolean isDefaultAddress) {
        this.isDefaultAddress = isDefaultAddress;
    }

    public String getBigAddress() {
        return this.bigAddress;
    }

    public void setBigAddress(String bigAddress) {
        this.bigAddress = bigAddress;
    }

    public String getSmallAddress() {
        return this.smallAddress;
    }

    public void setSmallAddress(String smallAddress) {
        this.smallAddress = smallAddress;
    }

    public String getAddress() {
        return this.address;
    }

    public void setAddress(String address) {
        this.address = address;
    }



}
