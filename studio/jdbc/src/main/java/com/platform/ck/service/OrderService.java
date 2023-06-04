package com.platform.ck.service;

import com.platform.ck.entity.Order;


import java.util.List;

/**
 * @author AllDataDC
 * @date 2023/01/05
 */
public interface OrderService {
    Integer getTotalAmountBySkuId(String skuId);
    List<Order> getOrderById(Integer id);

    int insertOrder(Order order);

    int deleteOrder(Integer id);

    int updateOrder(Order order);

}
