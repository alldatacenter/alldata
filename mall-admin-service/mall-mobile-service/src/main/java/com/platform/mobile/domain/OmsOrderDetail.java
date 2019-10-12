package com.platform.mobile.domain;

import com.platform.manage.model.OmsOrder;
import com.platform.manage.model.OmsOrderItem;

import java.util.List;

/**
 * 包含订单商品信息的订单详情
 * Created by wulinhao on 2019/9/4.
 */
public class OmsOrderDetail extends OmsOrder {
    private List<OmsOrderItem> orderItemList;

    public List<OmsOrderItem> getOrderItemList() {
        return orderItemList;
    }

    public void setOrderItemList(List<OmsOrderItem> orderItemList) {
        this.orderItemList = orderItemList;
    }
}
