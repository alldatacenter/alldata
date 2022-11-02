package com.platform.mall.mapper.admin;

import com.platform.mall.dto.admin.SmsCouponParam;
import org.apache.ibatis.annotations.Param;

/**
 * 优惠券管理自定义查询Dao
 * @author AllDataDC
 */
public interface SmsCouponDao {
    SmsCouponParam getItem(@Param("id") Long id);
}
