package com.platform.controller;

import com.platform.mall.common.CommonResult;
import com.platform.mall.entity.admin.HomePageData;
import com.platform.mall.entity.admin.OrderListData;
import com.platform.mall.service.admin.OmsOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;

/**
 * 首页数据初始化 Controller
 * Created by wulinhao on 2019/9/6.
 */
@Controller
@Api(tags = "SmsHomeDataController", description = "首页数据初始化")
@RequestMapping("/home")
public class SmsHomeDataController {
    @Autowired
    private OmsOrderService omsOrderService;

    @ApiOperation(value = "获取当前登录用户信息")
    @RequestMapping(value = "/homePageData", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult getHomePageData() {
        HomePageData homePageData = new HomePageData();
        //销售总额
        setSalesData(homePageData);
        //订单
        setOrderData(homePageData);
        //商品
        setGoodsData(homePageData);
        //用户总览
        setMembersData(homePageData);
        //增长率
        setGrowthRate(homePageData);
        //orderArray
        setOrderArray(homePageData);
        return CommonResult.success(homePageData);
    }

    private void setOrderArray(HomePageData homePageData) {
        OrderListData[] dataList = omsOrderService.getOrderListData();
        homePageData.setOrderArray(dataList);
    }

    private void setGrowthRate(HomePageData homePageData) {
        setWeekSalesOfGrowthRate(homePageData);
        setMonthSalesOfGrowthRate(homePageData);
        setWeekOrderOfGrowthRate(homePageData);
        setMonthOrderOfGrowthRate(homePageData);

    }

    private void setMonthOrderOfGrowthRate(HomePageData homePageData) {
        boolean flag = false;
        Integer monthTotalNumOfOrder = homePageData.getWeekTotalNumOfOrder();
        Integer lastWeekTotalNumOfOrder = homePageData.getLastWeekTotalNumOfOrder();
        Double monthOrdersOfGrowthRate;
        if (monthTotalNumOfOrder >= lastWeekTotalNumOfOrder){
            flag = true;
        }
        if (flag){
            if (monthTotalNumOfOrder == 0.0){
                return;
            }
            monthOrdersOfGrowthRate = (monthTotalNumOfOrder - lastWeekTotalNumOfOrder) / (monthTotalNumOfOrder * 1.0);
        }else{
            if (lastWeekTotalNumOfOrder == 0.0){
                return;
            }
            monthOrdersOfGrowthRate = (lastWeekTotalNumOfOrder - monthTotalNumOfOrder) / (lastWeekTotalNumOfOrder * 1.0);
        }
        monthOrdersOfGrowthRate = monthOrdersOfGrowthRate * 100;
        if (flag) {
            homePageData.setWeekOrderOfGrowthRate("+" + new BigDecimal(monthOrdersOfGrowthRate).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() + "%");
        }else {
            homePageData.setWeekOrderOfGrowthRate("-" + new BigDecimal(monthOrdersOfGrowthRate).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() + "%");
        }
    }

    private void setWeekOrderOfGrowthRate(HomePageData homePageData) {
        boolean flag = false;
        Integer weekTotalNumOfOrder = homePageData.getMonthTotalNumOfOrder();
        Integer lastMonthTotalNumOfOrder = homePageData.getLastMonthTotalNumOfOrder();
        Double weekOrdersOfGrowthRate;
        if (weekTotalNumOfOrder >= lastMonthTotalNumOfOrder){
            flag = true;
        }
        if (flag){
            if (weekTotalNumOfOrder == 0.0){
                return;
            }
            weekOrdersOfGrowthRate = (weekTotalNumOfOrder - lastMonthTotalNumOfOrder) / (weekTotalNumOfOrder * 1.0);
        }else{
            if (lastMonthTotalNumOfOrder == 0.0){
                return;
            }
            weekOrdersOfGrowthRate = (lastMonthTotalNumOfOrder - weekTotalNumOfOrder) / (lastMonthTotalNumOfOrder * 1.0);
        }
        weekOrdersOfGrowthRate = weekOrdersOfGrowthRate * 100;
        if (flag) {
            homePageData.setMonthOrderOfGrowthRate("+" + new BigDecimal(weekOrdersOfGrowthRate).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() + "%");
        }else {
            homePageData.setMonthOrderOfGrowthRate("-" + new BigDecimal(weekOrdersOfGrowthRate).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() + "%");
        }
    }

    private void setMonthSalesOfGrowthRate(HomePageData homePageData) {
        boolean flag = false;
        Double totalSalesOfMonth = homePageData.getTotalSalesOfMonth();
        Double totalSalesOfLastMonth = homePageData.getTotalSalesOfLastMonth();
        Double monthSalesOfGrowthRate;
        if (totalSalesOfMonth >= totalSalesOfLastMonth){
            flag = true;
        }
        if (flag){
            if (totalSalesOfMonth == 0.0){
                return;
            }
            monthSalesOfGrowthRate = totalSalesOfMonth - totalSalesOfLastMonth / totalSalesOfMonth;
        }else{
            if (totalSalesOfLastMonth == 0.0){
                return;
            }
            monthSalesOfGrowthRate = totalSalesOfLastMonth - totalSalesOfMonth / totalSalesOfLastMonth;
        }
        monthSalesOfGrowthRate = monthSalesOfGrowthRate * 100;
        if (flag) {
            homePageData.setMonthSalesOfGrowthRate("+" + new BigDecimal(monthSalesOfGrowthRate).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() + "%");
        }else {
            homePageData.setMonthSalesOfGrowthRate("-" + new BigDecimal(monthSalesOfGrowthRate).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() + "%");
        }
    }

    private void setWeekSalesOfGrowthRate(HomePageData homePageData) {
        boolean flag = false;
        Double totalSalesOfWeek = homePageData.getTotalSalesOfWeek();
        Double totalSalesOfLastWeek = homePageData.getTotalSalesOfLastWeek();
        Double weekSalesOfGrowthRate;
        if (totalSalesOfWeek >= totalSalesOfLastWeek){
            flag = true;
        }
        if (flag){
            if (totalSalesOfWeek == 0.0){
                return;
            }
            weekSalesOfGrowthRate = totalSalesOfWeek - totalSalesOfLastWeek / totalSalesOfWeek;
        }else{
            if (totalSalesOfLastWeek == 0.0){
                return;
            }
            weekSalesOfGrowthRate = totalSalesOfLastWeek - totalSalesOfWeek / totalSalesOfLastWeek;
        }
        weekSalesOfGrowthRate = weekSalesOfGrowthRate * 100;
        if (flag) {
            homePageData.setWeekSalesOfGrowthRate("+" + new BigDecimal(weekSalesOfGrowthRate).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() + "%");
        }else {
            homePageData.setWeekSalesOfGrowthRate("-" + new BigDecimal(weekSalesOfGrowthRate).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() + "%");
        }
    }

    private void setMembersData(HomePageData homePageData) {
        Integer addToday = omsOrderService.getAddToday();
        homePageData.setOffShelfGoods(addToday);
        Integer addYestoday = omsOrderService.getAddYestoday();
        homePageData.setOnShelfGoods(addYestoday);
        Integer addMonth  = omsOrderService.getAddMonth();
        homePageData.setTightStockGoods(addMonth);
        Integer allMembers = omsOrderService.getAllMembers();
        homePageData.setAllGoods(allMembers);
    }

    private void setGoodsData(HomePageData homePageData) {
        Integer offShelfGoods = omsOrderService.getOffShelfGoods();
        homePageData.setOffShelfGoods(offShelfGoods);
        Integer onShelfGoods = omsOrderService.getOnShelfGoods();
        homePageData.setOnShelfGoods(onShelfGoods);
        Integer tightStockGoods  = omsOrderService.getTightStockGoods();
        homePageData.setTightStockGoods(tightStockGoods);
        Integer allGoods = omsOrderService.getAllGoods();
        homePageData.setAllGoods(allGoods);
    }

    private void setOrderData(HomePageData homePageData) {
        //待付款订单
        Integer numOfWaitForPay = omsOrderService.getNumOfWaitForPay();
        homePageData.setNumOfWaitForPay(numOfWaitForPay);

        //已完成订单
        Integer numOfFinished = omsOrderService.getNumOfFinished();
        homePageData.setNumOfFinished(numOfFinished);

        //待确认收货订单
        Integer numOfWaitForConfirmRecvice = omsOrderService.getNumOfWaitForConfirmRecvice();
        homePageData.setNumOfWaitForConfirmRecvice(numOfWaitForConfirmRecvice);

        Integer numOfWaitForDeliverGoods = omsOrderService.getNumOfWaitForDeliverGoods();
        homePageData.setNumOfWaitForDeliverGoods(numOfWaitForDeliverGoods);

        //新缺货登记
        Integer numOfNewShortageRegistration = omsOrderService.getNumOfNewShortageRegistration();
        homePageData.setNumOfNewShortageRegistration(numOfNewShortageRegistration);

        //待处理退款申请
        Integer numOfWaitForRefundApplication = omsOrderService.getNumOfWaitForRefundApplication();
        homePageData.setNumOfWaitForRefundApplication(numOfWaitForRefundApplication);

        //已发货订单
        Integer numOfOutgoingOrders = omsOrderService.getNumOfOutgoingOrders();
        homePageData.setNumOfOutgoingOrders(numOfOutgoingOrders);

        //待处理退货订单
        Integer returnOrdersToBeProcessed = omsOrderService.getReturnOrdersToBeProcessed();
        homePageData.setReturnOrdersToBeProcessed(returnOrdersToBeProcessed);

        //广告位即将到期
        Integer advertisingSpaceNealyExpire = omsOrderService.getAdvertisingSpaceNealyExpire();
        homePageData.setAdvertisingSpaceNealyExpire(advertisingSpaceNealyExpire);

        //今日订单总数
        Integer todayTotalNumOfOrder = omsOrderService.getTodayTotalNumOfOrder();
        homePageData.setTodayTotalNumOfOrder(todayTotalNumOfOrder);

        //本月订单总数
        Integer monthTotalNumOfOrder = omsOrderService.getMonthTotalNumOfOrder();
        homePageData.setMonthTotalNumOfOrder(monthTotalNumOfOrder);

        //本周订单总数
        Integer weekTotalNumOfOrder = omsOrderService.getWeekTotalNumOfOrder();
        homePageData.setWeekTotalNumOfOrder(weekTotalNumOfOrder);

        //上月订单总数
        Integer lastMonthTotalNumOfOrder = omsOrderService.getLastMonthTotalNumOfOrder();
        homePageData.setMonthTotalNumOfOrder(lastMonthTotalNumOfOrder);

        //上周订单总数
        Integer lastWeekTotalNumOfOrder = omsOrderService.getLastWeekTotalNumOfOrder();
        homePageData.setWeekTotalNumOfOrder(lastWeekTotalNumOfOrder);
    }

    private void setSalesData(HomePageData homePageData) {
        //今日销售总额
        Double totalSalesOfToday = omsOrderService.getTotalSalesOfToday();
        homePageData.setTotalSalesOfToday(totalSalesOfToday);
        //昨日销售总额
        Double totalSalesOfYestoday = omsOrderService.getTotalSalesOfYestoday();
        homePageData.setTotalSalesOfYestoday(totalSalesOfYestoday);
        //近7天销售总额
        Double totalSalesOfNearly7Days = omsOrderService.getTotalSalesOfNearly7Days();
        homePageData.setTotalSalesOfNearly7Days(totalSalesOfNearly7Days);
        //本周销售总额
        Double totalSalesOfWeek = omsOrderService.getTotalSalesOfWeek();
        homePageData.setTotalSalesOfWeek(totalSalesOfWeek);
        //本月销售总额
        Double totalSalesOfMonth = omsOrderService.getTotalSalesOfMonth();
        homePageData.setTotalSalesOfMonth(totalSalesOfMonth);
        //上周销售总额
        Double totalSalesOfLastWeek = omsOrderService.getTotalSalesOfLastWeek();
        homePageData.setTotalSalesOfWeek(totalSalesOfLastWeek);
        //上月销售总额
        Double totalSalesOfLastMonth = omsOrderService.getTotalSalesOfLastMonth();
        homePageData.setTotalSalesOfMonth(totalSalesOfLastMonth);

    }
}
