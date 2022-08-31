package com.platform.mall.service;

import com.platform.mall.dto.front.Order;
import com.platform.mall.dto.front.OrderInfo;
import com.platform.mall.dto.front.PageOrder;
import com.platform.mall.entity.DataTablesResult;
import com.platform.mall.dto.OrderDetail;
import com.platform.mall.entity.TbThanks;

import java.math.BigDecimal;


/**
 * @author wulinhao
 */
public interface OrderService {

    /**
     * 获得订单列表
     * @param draw
     * @param start
     * @param length
     * @param search
     * @param orderCol
     * @param orderDir
     * @return
     */
    DataTablesResult getOrderList(int draw, int start, int length, String search, String orderCol, String orderDir);

    /**
     * 统计订单数
     * @return
     */
    Long countOrder();

    /**
     * 获取订单详情
     * @param orderId
     * @return
     */
    OrderDetail getOrderDetail(String orderId);

    /**
     * 发货
     * @param orderId
     * @param shippingName
     * @param shippingCode
     * @param postFee
     * @return
     */
    int deliver(String orderId,String shippingName,String shippingCode,BigDecimal postFee);

    /**
     * 备注
     * @param orderId
     * @param message
     * @return
     */
    int remark(String orderId, String message);

    /**
     * 取消订单
     * @param orderId
     * @return
     */
    int cancelOrderByAdmin(String orderId);

    /**
     * 删除订单
     * @param id
     * @return
     */
    int deleteOrder(String id);

    /**
     * 定时取消订单
     */
    int cancelOrder();

    /**
     * 支付审核通过
     * @param tokenName
     * @param token
     * @param id
     * @return
     */
    int passPay(String tokenName,String token,String id);

    /**
     * 支付审核驳回
     * @param tokenName
     * @param token
     * @param id
     * @return
     */
    int backPay(String tokenName,String token,String id);

    /**
     * 支付审核通过不显示
     * @param tokenName
     * @param token
     * @param id
     * @return
     */
    int notShowPay(String tokenName,String token,String id);

    /**
     * 捐赠编辑
     * @param tokenName
     * @param token
     * @param tbThanks
     * @return
     */
    int editPay(String tokenName,String token,TbThanks tbThanks);

    /**
     * 捐赠删除不回邮件
     * @param tokenName
     * @param token
     * @param id
     * @return
     */
    int payDelNotNotify(String tokenName,String token,String id);

    /**
     * 捐赠删除
     * @param tokenName
     * @param token
     * @param id
     * @return
     */
    int payDel(String tokenName,String token,String id);

    /**
     * 分页获得用户订单
     * @param userId
     * @param page
     * @param size
     * @return
     */
    PageOrder getOrderList(Long userId, int page, int size);

    /**
     * 获得单个订单
     * @param orderId
     * @return
     */
    Order getOrder(Long orderId);

    /**
     * 取消订单
     * @param orderId
     * @return
     */
    int cancelOrder(Long orderId);

    /**
     * 创建订单
     * @param orderInfo
     * @return
     */
    Long createOrder(OrderInfo orderInfo);

    /**
     * 删除订单
     * @param orderId
     * @return
     */
    int delOrder(Long orderId);

    /**
     * 订单支付 生成捐赠数据
     * @param tbThanks
     * @return
     */
    int payOrder(TbThanks tbThanks);
}
