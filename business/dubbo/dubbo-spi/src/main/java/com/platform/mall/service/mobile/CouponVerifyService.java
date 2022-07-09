package com.platform.mall.service.mobile;


import com.platform.mall.entity.mobile.LitemallCoupon;

import java.math.BigDecimal;

public interface CouponVerifyService {
    /**
     * 检测优惠券是否适合
     *
     * @param userId
     * @param couponId
     * @param checkedGoodsPrice
     * @return
     */
    LitemallCoupon checkCoupon(Integer userId, Integer couponId, Integer userCouponId, BigDecimal checkedGoodsPrice);
}
