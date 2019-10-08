package com.platform.dao;

import com.platform.dto.SmsCouponParam;
import org.apache.ibatis.annotations.Param;

/**
 * 优惠券管理自定义查询Dao
 * Created by wulinhao on 2019/8/29.
 */
public interface SmsCouponDao {
    SmsCouponParam getItem(@Param("id") Long id);
}
