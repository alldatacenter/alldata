package com.platform.manage.dto;

import com.platform.mbg.model.OmsOrder;
import com.platform.mbg.model.OmsOrderItem;
import com.platform.mbg.model.OmsOrderOperateHistory;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 订单详情信息
 * Created by wulinhao on 2019/9/11.
 */
public class OmsOrderDetail extends OmsOrder {
    @Getter
    @Setter
    private List<OmsOrderItem> orderItemList;
    @Getter
    @Setter
    private List<OmsOrderOperateHistory> historyList;
}
