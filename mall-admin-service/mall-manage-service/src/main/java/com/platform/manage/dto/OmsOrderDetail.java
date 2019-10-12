package com.platform.manage.dto;

import com.platform.manage.model.OmsOrder;
import com.platform.manage.model.OmsOrderItem;
import com.platform.manage.model.OmsOrderOperateHistory;
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
