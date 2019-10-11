package com.platform.base.dao;

import com.platform.base.domain.SmsCouponHistoryDetail;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 会员优惠券领取历史自定义Dao
 * Created by wulinhao on 2019/8/29.
 */
public interface SmsCouponHistoryDao {
    List<SmsCouponHistoryDetail> getDetailList(@Param("memberId") Long memberId);
}
