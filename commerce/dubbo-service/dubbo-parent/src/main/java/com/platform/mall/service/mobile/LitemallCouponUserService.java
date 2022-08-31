package com.platform.mall.service.mobile;

import com.platform.mall.entity.mobile.LitemallCouponUser;

import java.util.List;

public interface LitemallCouponUserService {
    Integer countCoupon(Integer couponId);

    Integer countUserAndCoupon(Integer userId, Integer couponId);

    void add(LitemallCouponUser couponUser);

    List<LitemallCouponUser> queryList(Integer userId, Integer couponId, Short status, Integer page, Integer size, String sort, String order);

    List<LitemallCouponUser> queryAll(Integer userId, Integer couponId);

    List<LitemallCouponUser> queryAll(Integer userId);

    LitemallCouponUser queryOne(Integer userId, Integer couponId);

    LitemallCouponUser findById(Integer id);

    int update(LitemallCouponUser couponUser);

    List<LitemallCouponUser> queryExpired();
}
