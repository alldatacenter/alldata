package com.platform.mall.service.admin;

import com.platform.mall.dto.admin.OmsOrderReturnApplyResult;
import com.platform.mall.dto.admin.OmsReturnApplyQueryParam;
import com.platform.mall.dto.admin.OmsUpdateStatusParam;
import com.platform.mall.entity.admin.OmsOrderReturnApply;

import java.util.List;

/**
 * 退货申请管理Service
 * Created by wulinhao on 2019/9/18.
 */
public interface OmsOrderReturnApplyService {
    /**
     * 分页查询申请
     */
    List<OmsOrderReturnApply> list(OmsReturnApplyQueryParam queryParam, Integer pageSize, Integer pageNum);

    /**
     * 批量删除申请
     */
    int delete(List<Long> ids);

    /**
     * 修改申请状态
     */
    int updateStatus(Long id, OmsUpdateStatusParam statusParam);

    /**
     * 获取指定申请详情
     */
    OmsOrderReturnApplyResult getItem(Long id);
}
