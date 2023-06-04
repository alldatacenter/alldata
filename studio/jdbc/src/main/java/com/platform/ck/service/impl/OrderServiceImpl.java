package com.platform.ck.service.impl;

import com.platform.ck.entity.Order;
import com.platform.ck.mapper.OrderMapper;
import com.platform.ck.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.List;

/**
 * @author AllDataDC
 * @date 2023/01/05
 */
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Override
    public Integer getTotalAmountBySkuId(String skuId) {
        return orderMapper.selectTotalAmountSkuId(skuId);
    }

    @Override
    public List<Order> getOrderById(Integer id) {
        return orderMapper.selectOrder(id);
    }

    @Override
    public int insertOrder(Order order) {
        int result = orderMapper.insertOrder(order);
        return result;
    }

    @Override
    public int deleteOrder(Integer id) {
        int result = orderMapper.deleteOrder(id);
        return result;
    }

    @Override
    public int updateOrder(Order order) {
        int result = orderMapper.updateOrder(order);
        return result;
    }
}
