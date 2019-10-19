package com.platform.mall.dto.admin;

import com.platform.mall.entity.admin.OmsOrder;
import com.platform.mall.entity.admin.OmsOrderItem;
import com.platform.mall.entity.admin.OmsOrderOperateHistory;

import java.io.Serializable;
import java.util.List;

/**
 * 订单详情信息
 * Created by wulinhao on 2019/9/11.
 */
public class OmsOrderDetail extends OmsOrder  implements Serializable {

    private List<OmsOrderItem> orderItemList;
    private List<OmsOrderOperateHistory> historyList;

    public List<OmsOrderItem> getOrderItemList() {
        return orderItemList;
    }

    public void setOrderItemList(List<OmsOrderItem> orderItemList) {
        this.orderItemList = orderItemList;
    }

    public List<OmsOrderOperateHistory> getHistoryList() {
        return historyList;
    }

    public void setHistoryList(List<OmsOrderOperateHistory> historyList) {
        this.historyList = historyList;
    }
}
