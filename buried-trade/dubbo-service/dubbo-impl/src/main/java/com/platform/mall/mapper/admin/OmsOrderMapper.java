package com.platform.mall.mapper.admin;

import com.platform.mall.entity.admin.OmsOrder;
import com.platform.mall.entity.admin.OmsOrderExample;

import java.util.List;

import com.platform.mall.entity.admin.OrderListData;
import org.apache.ibatis.annotations.Param;

public interface OmsOrderMapper {
    long countByExample(OmsOrderExample example);

    int deleteByExample(OmsOrderExample example);

    int deleteByPrimaryKey(Long id);

    int insert(OmsOrder record);

    int insertSelective(OmsOrder record);

    List<OmsOrder> selectByExample(OmsOrderExample example);

    OmsOrder selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") OmsOrder record, @Param("example") OmsOrderExample example);

    int updateByExample(@Param("record") OmsOrder record, @Param("example") OmsOrderExample example);

    int updateByPrimaryKeySelective(OmsOrder record);

    int updateByPrimaryKey(OmsOrder record);

    Double getTotalSalesOfToday();

    Double getTotalSalesOfYesToday();

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
