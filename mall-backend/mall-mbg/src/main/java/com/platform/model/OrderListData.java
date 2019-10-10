package com.platform.model;

/**
 * Created with IntelliJ IDEA.
 * Description: 首頁列表date, orderCount, orderAmount
 * User: wulinhao
 * Date: 2019-10-10
 * Time: 14:30
 */
public class OrderListData {
    private String date;
    private Integer orderCount;
    private Integer orderAmount;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Integer getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Integer orderCount) {
        this.orderCount = orderCount;
    }

    public Integer getOrderAmount() {
        return orderAmount;
    }

    public void setOrderAmount(Integer orderAmount) {
        this.orderAmount = orderAmount;
    }
}
