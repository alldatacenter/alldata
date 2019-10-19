package com.platform.mall.mapper.admin;

import com.platform.mall.dto.admin.OmsOrderReturnApplyResult;
import com.platform.mall.dto.admin.OmsReturnApplyQueryParam;
import com.platform.mall.entity.admin.OmsOrderReturnApply;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单退货申请自定义Dao
 * Created by wulinhao on 2019/9/18.
 */
public interface OmsOrderReturnApplyDao {
    /**
     * 查询申请列表
     */
    List<OmsOrderReturnApply> getList(@Param("queryParam") OmsReturnApplyQueryParam queryParam);

    /**
     * 获取申请详情
     */
    OmsOrderReturnApplyResult getDetail(@Param("id") Long id);
}
