package com.platform.controller;

import com.platform.common.api.CommonResult;
import com.platform.model.HomePageData;
import com.platform.service.OmsOrderService;
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

        return CommonResult.success(homePageData);
    }

    private void setGrowthRate(HomePageData homePageData) {
        setWeekSalesOfGrowthRate(homePageData);
        setMonthSalesOfGrowthRate(homePageData);
        setWeekOrderOfGrowthRate(homePageData);
        setMonthOrderOfGrowthRate(homePageData);
    
    }

    private void setMonthOrderOfGrowthRate(HomePageData homePageData) {
    }

    private void setWeekOrderOfGrowthRate(HomePageData homePageData) {
    }

    private void setMonthSalesOfGrowthRate(HomePageData homePageData) {
        double monthSalesOfGrowthRate =
                homePageData.getTotalSalesOfMonth() > homePageData.getTotalSalesOfLastMonth()
                        ? (homePageData.getTotalSalesOfMonth() - homePageData.getTotalSalesOfLastMonth()) / homePageData.getTotalSalesOfMonth()
                        : (homePageData.getTotalSalesOfLastMonth()-homePageData.getTotalSalesOfMonth()) / homePageData.getTotalSalesOfLastMonth();
        monthSalesOfGrowthRate = monthSalesOfGrowthRate * 100;
        if (homePageData.getTotalSalesOfMonth() > homePageData.getTotalSalesOfLastMonth()) {
            homePageData.setMonthSalesOfGrowthRate("+" + new BigDecimal(monthSalesOfGrowthRate).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() + "%");
        }else {
            homePageData.setMonthSalesOfGrowthRate("-" + new BigDecimal(monthSalesOfGrowthRate).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() + "%");
        }
    }

    private void setWeekSalesOfGrowthRate(HomePageData homePageData) {
        double weekSalesOfGrowthRate =
                homePageData.getTotalSalesOfWeek() > homePageData.getTotalSalesOfLastWeek()
                        ? (homePageData.getTotalSalesOfWeek() - homePageData.getTotalSalesOfLastWeek()) / homePageData.getTotalSalesOfWeek()
                        : (homePageData.getTotalSalesOfLastWeek()-homePageData.getTotalSalesOfWeek()) / homePageData.getTotalSalesOfLastWeek();
        weekSalesOfGrowthRate = weekSalesOfGrowthRate * 100;
        if (homePageData.getTotalSalesOfWeek() > homePageData.getTotalSalesOfLastWeek()) {
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
    }
}
