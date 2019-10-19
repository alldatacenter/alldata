package com.platform.mall.service.admin;

import com.platform.mall.dto.admin.*;
import com.platform.mall.entity.admin.OmsOrder;
import com.platform.mall.entity.admin.OrderListData;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 订单管理Service
 * Created by wulinhao on 2019/9/11.
 */
public interface OmsOrderService {
    /**
     * 订单查询
     */
    List<OmsOrder> list(OmsOrderQueryParam queryParam, Integer pageSize, Integer pageNum);

    /**
     * 批量发货
     */
    @Transactional
    int delivery(List<OmsOrderDeliveryParam> deliveryParamList);

    /**
     * 批量关闭订单
     */
    @Transactional
    int close(List<Long> ids, String note);

    /**
     * 批量删除订单
     */
    int delete(List<Long> ids);

    /**
     * 获取指定订单详情
     */
    OmsOrderDetail detail(Long id);

    /**
     * 修改订单收货人信息
     */
    @Transactional
    int updateReceiverInfo(OmsReceiverInfoParam receiverInfoParam);

    /**
     * 修改订单费用信息
     */
    @Transactional
    int updateMoneyInfo(OmsMoneyInfoParam moneyInfoParam);

    /**
     * 修改订单备注
     */
    @Transactional
    int updateNote(Long id, String note, Integer status);

    /**
     * 今日销售总额
     * @return
     */
    Double getTotalSalesOfToday();

    /**
     * 昨日销售总额
     * @return
     */
    Double getTotalSalesOfYestoday();

    Double getTotalSalesOfNearly7Days();

    Double getTotalSalesOfWeek();

    Double getTotalSalesOfMonth();

    Integer getNumOfWaitForPay();

    Integer getNumOfFinished();

    Integer getNumOfWaitForConfirmRecvice();

    Integer getNumOfWaitForDeliverGoods();

    Integer getNumOfNewShortageRegistration();

    Integer getNumOfWaitForRefundApplication();

    Integer getNumOfOutgoingOrders();

    Integer getReturnOrdersToBeProcessed();

    Integer getAdvertisingSpaceNealyExpire();

    Integer getTodayTotalNumOfOrder();

    Integer getMonthTotalNumOfOrder();

    Integer getWeekTotalNumOfOrder();

    Integer getOffShelfGoods();

    Integer getOnShelfGoods();

    Integer getTightStockGoods();

    Integer getAllGoods();

    Integer getAddToday();

    Integer getAddYestoday();

    Integer getAddMonth();

    Integer getAllMembers();

    Double getTotalSalesOfLastWeek();

    Double getTotalSalesOfLastMonth();

    Integer getLastWeekTotalNumOfOrder();

    Integer getLastMonthTotalNumOfOrder();

    OrderListData[] getOrderListData();
}
