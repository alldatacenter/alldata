package com.platform.mall.entity.admin;

import java.io.Serializable;

public class HomePageData implements Serializable {
    /**
     * 销售总额
     * 今日销售总额
     * 昨日销售总额
     * 近7天销售总额
     * 本周销售总额
     * 本月销售总额
     * 上周销售总额
     * 上月销售总额
     */
    private double totalSalesOfToday;
    private double totalSalesOfYestoday;
    private double totalSalesOfNearly7Days;
    private double totalSalesOfWeek;
    private double totalSalesOfMonth;
    private double totalSalesOfLastWeek;
    private double totalSalesOfLastMonth;

    /**
     * 订单
     * 待付款订单
     * 已完成订单
     * 待确认收货订单
     * 待发货订单
     * 新缺货登记
     * 待处理退款申请
     * 已发货订单
     * 待处理退货订单
     * 广告位即将到期
     * 今日订单总数
     * 本月订单总数
     * 本周订单总数
     * 上月订单总数
     * 上周订单总数
     */
    private int numOfWaitForPay;
    private int numOfFinished;
    private int numOfWaitForConfirmRecvice;
    private int numOfWaitForDeliverGoods;
    private int numOfNewShortageRegistration;
    private int numOfWaitForRefundApplication;
    private int numOfOutgoingOrders;
    private int returnOrdersToBeProcessed;
    private int advertisingSpaceNealyExpire;
    private int todayTotalNumOfOrder;
    private int monthTotalNumOfOrder;
    private int weekTotalNumOfOrder;
    private int lastMonthTotalNumOfOrder;
    private int lastWeekTotalNumOfOrder;

    /**
     * 同比上周订单增长率
     */
    private String weekOrderOfGrowthRate;
    /**
     * 同比上月订单增长率
     */
    private String monthOrderOfGrowthRate;

    /**
     * 同比上周销售总额增长率
     */
    private String weekSalesOfGrowthRate;


    /**
     * 同比上月销售总额增长率
     */
    private String monthSalesOfGrowthRate;

    /**
     * 商品
     * 已下架、已上架、库存紧张、全部商品
     */
    private int offShelfGoods;
    private int onShelfGoods;
    private int tightStockGoods;
    private int allGoods;

    /**
     * 用户总览
     * 今日新增、昨日新增、本月新增、会员总数
     */
    private int addToday;
    private int addYestoday;
    private int addMonth;
    private int allMembers;
    private OrderListData[] orderArray;


    public OrderListData[] getOrderArray() {
        return orderArray;
    }

    public void setOrderArray(OrderListData[] orderArray) {
        this.orderArray = orderArray;
    }

    public double getTotalSalesOfToday() {
        return totalSalesOfToday;
    }

    public void setTotalSalesOfToday(double totalSalesOfToday) {
        this.totalSalesOfToday = totalSalesOfToday;
    }

    public double getTotalSalesOfYestoday() {
        return totalSalesOfYestoday;
    }

    public void setTotalSalesOfYestoday(double totalSalesOfYestoday) {
        this.totalSalesOfYestoday = totalSalesOfYestoday;
    }

    public double getTotalSalesOfNearly7Days() {
        return totalSalesOfNearly7Days;
    }

    public void setTotalSalesOfNearly7Days(double totalSalesOfNearly7Days) {
        this.totalSalesOfNearly7Days = totalSalesOfNearly7Days;
    }

    public double getTotalSalesOfWeek() {
        return totalSalesOfWeek;
    }

    public void setTotalSalesOfWeek(double totalSalesOfWeek) {
        this.totalSalesOfWeek = totalSalesOfWeek;
    }

    public double getTotalSalesOfMonth() {
        return totalSalesOfMonth;
    }

    public void setTotalSalesOfMonth(double totalSalesOfMonth) {
        this.totalSalesOfMonth = totalSalesOfMonth;
    }

    public int getNumOfWaitForPay() {
        return numOfWaitForPay;
    }

    public void setNumOfWaitForPay(int numOfWaitForPay) {
        this.numOfWaitForPay = numOfWaitForPay;
    }

    public int getNumOfFinished() {
        return numOfFinished;
    }

    public void setNumOfFinished(int numOfFinished) {
        this.numOfFinished = numOfFinished;
    }

    public int getNumOfWaitForConfirmRecvice() {
        return numOfWaitForConfirmRecvice;
    }

    public void setNumOfWaitForConfirmRecvice(int numOfWaitForConfirmRecvice) {
        this.numOfWaitForConfirmRecvice = numOfWaitForConfirmRecvice;
    }

    public int getNumOfWaitForDeliverGoods() {
        return numOfWaitForDeliverGoods;
    }

    public void setNumOfWaitForDeliverGoods(int numOfWaitForDeliverGoods) {
        this.numOfWaitForDeliverGoods = numOfWaitForDeliverGoods;
    }

    public int getNumOfNewShortageRegistration() {
        return numOfNewShortageRegistration;
    }

    public void setNumOfNewShortageRegistration(int numOfNewShortageRegistration) {
        this.numOfNewShortageRegistration = numOfNewShortageRegistration;
    }

    public int getNumOfWaitForRefundApplication() {
        return numOfWaitForRefundApplication;
    }

    public void setNumOfWaitForRefundApplication(int numOfWaitForRefundApplication) {
        this.numOfWaitForRefundApplication = numOfWaitForRefundApplication;
    }

    public int getNumOfOutgoingOrders() {
        return numOfOutgoingOrders;
    }

    public void setNumOfOutgoingOrders(int numOfOutgoingOrders) {
        this.numOfOutgoingOrders = numOfOutgoingOrders;
    }

    public int getReturnOrdersToBeProcessed() {
        return returnOrdersToBeProcessed;
    }

    public void setReturnOrdersToBeProcessed(int returnOrdersToBeProcessed) {
        this.returnOrdersToBeProcessed = returnOrdersToBeProcessed;
    }

    public int getAdvertisingSpaceNealyExpire() {
        return advertisingSpaceNealyExpire;
    }

    public void setAdvertisingSpaceNealyExpire(int advertisingSpaceNealyExpire) {
        this.advertisingSpaceNealyExpire = advertisingSpaceNealyExpire;
    }

    public int getTodayTotalNumOfOrder() {
        return todayTotalNumOfOrder;
    }

    public void setTodayTotalNumOfOrder(int todayTotalNumOfOrder) {
        this.todayTotalNumOfOrder = todayTotalNumOfOrder;
    }

    public int getMonthTotalNumOfOrder() {
        return monthTotalNumOfOrder;
    }

    public void setMonthTotalNumOfOrder(int monthTotalNumOfOrder) {
        this.monthTotalNumOfOrder = monthTotalNumOfOrder;
    }

    public int getWeekTotalNumOfOrder() {
        return weekTotalNumOfOrder;
    }

    public void setWeekTotalNumOfOrder(int weekTotalNumOfOrder) {
        this.weekTotalNumOfOrder = weekTotalNumOfOrder;
    }

    public int getOffShelfGoods() {
        return offShelfGoods;
    }

    public void setOffShelfGoods(int offShelfGoods) {
        this.offShelfGoods = offShelfGoods;
    }

    public int getOnShelfGoods() {
        return onShelfGoods;
    }

    public void setOnShelfGoods(int onShelfGoods) {
        this.onShelfGoods = onShelfGoods;
    }

    public int getTightStockGoods() {
        return tightStockGoods;
    }

    public void setTightStockGoods(int tightStockGoods) {
        this.tightStockGoods = tightStockGoods;
    }

    public int getAllGoods() {
        return allGoods;
    }

    public void setAllGoods(int allGoods) {
        this.allGoods = allGoods;
    }

    public int getAddToday() {
        return addToday;
    }

    public void setAddToday(int addToday) {
        this.addToday = addToday;
    }

    public int getAddYestoday() {
        return addYestoday;
    }

    public void setAddYestoday(int addYestoday) {
        this.addYestoday = addYestoday;
    }

    public int getAddMonth() {
        return addMonth;
    }

    public void setAddMonth(int addMonth) {
        this.addMonth = addMonth;
    }

    public int getAllMembers() {
        return allMembers;
    }

    public void setAllMembers(int allMembers) {
        this.allMembers = allMembers;
    }

    public String getWeekOrderOfGrowthRate() {
        return weekOrderOfGrowthRate;
    }

    public void setWeekOrderOfGrowthRate(String weekOrderOfGrowthRate) {
        this.weekOrderOfGrowthRate = weekOrderOfGrowthRate;
    }

    public String getMonthOrderOfGrowthRate() {
        return monthOrderOfGrowthRate;
    }

    public void setMonthOrderOfGrowthRate(String monthOrderOfGrowthRate) {
        this.monthOrderOfGrowthRate = monthOrderOfGrowthRate;
    }

    public String getWeekSalesOfGrowthRate() {
        return weekSalesOfGrowthRate;
    }

    public void setWeekSalesOfGrowthRate(String weekSalesOfGrowthRate) {
        this.weekSalesOfGrowthRate = weekSalesOfGrowthRate;
    }

    public String getMonthSalesOfGrowthRate() {
        return monthSalesOfGrowthRate;
    }

    public void setMonthSalesOfGrowthRate(String monthSalesOfGrowthRate) {
        this.monthSalesOfGrowthRate = monthSalesOfGrowthRate;
    }

    public double getTotalSalesOfLastWeek() {
        return totalSalesOfLastWeek;
    }

    public void setTotalSalesOfLastWeek(double totalSalesOfLastWeek) {
        this.totalSalesOfLastWeek = totalSalesOfLastWeek;
    }

    public double getTotalSalesOfLastMonth() {
        return totalSalesOfLastMonth;
    }

    public void setTotalSalesOfLastMonth(double totalSalesOfLastMonth) {
        this.totalSalesOfLastMonth = totalSalesOfLastMonth;
    }


    public int getLastMonthTotalNumOfOrder() {
        return lastMonthTotalNumOfOrder;
    }

    public void setLastMonthTotalNumOfOrder(int lastMonthTotalNumOfOrder) {
        this.lastMonthTotalNumOfOrder = lastMonthTotalNumOfOrder;
    }

    public void setLastWeekTotalNumOfOrder(int lastWeekTotalNumOfOrder) {
        this.lastWeekTotalNumOfOrder = lastWeekTotalNumOfOrder;
    }
    public int getLastWeekTotalNumOfOrder() {
        return lastWeekTotalNumOfOrder;
    }
}
