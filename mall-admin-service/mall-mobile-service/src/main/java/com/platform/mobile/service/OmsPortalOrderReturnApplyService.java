package com.platform.mobile.service;

import com.platform.mobile.domain.OmsOrderReturnApplyParam;

/**
 * 订单退货管理Service
 * Created by wulinhao on 2019/9/17.
 */
public interface OmsPortalOrderReturnApplyService {
    /**
     * 提交申请
     */
    int create(OmsOrderReturnApplyParam returnApply);
}
