package com.platform.mall.service.mobile;

import com.platform.mall.entity.mobile.LitemallCoupon;
import com.platform.mall.entity.mobile.LitemallCouponExample;

import java.util.List;

public interface LitemallCouponService {
    /**
     * 查询，空参数
     *
     * @param offset
     * @param limit
     * @param sort
     * @param order
     * @return
     */
    List<LitemallCoupon> queryList(int offset, int limit, String sort, String order);

    /**
     * 查询
     *
     * @param criteria 可扩展的条件
     * @param offset
     * @param limit
     * @param sort
     * @param order
     * @return
     */
    List<LitemallCoupon> queryList(LitemallCouponExample.Criteria criteria, int offset, int limit,
                                   String sort, String order);

    List<LitemallCoupon> queryAvailableList(Integer userId, int offset, int limit);

    List<LitemallCoupon> queryList(int offset, int limit);

    LitemallCoupon findById(Integer id);


    LitemallCoupon findByCode(String code);

    /**
     * 查询新用户注册优惠券
     *
     * @return
     */
    List<LitemallCoupon> queryRegister();

    List<LitemallCoupon> querySelective(String name, Short type, Short status,
                                        Integer page, Integer limit, String sort, String order);

    void add(LitemallCoupon coupon);

    int updateById(LitemallCoupon coupon);

    void deleteById(Integer id);

    /**
     * 生成优惠码
     *
     * @return 可使用优惠码
     */
    String generateCode();

    /**
     * 查询过期的优惠券:
     * 注意：如果timeType=0, 即基于领取时间有效期的优惠券，则优惠券不会过期
     *
     * @return
     */
    List<LitemallCoupon> queryExpired();
}
