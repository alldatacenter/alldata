package com.platform.ck.controller;

import com.platform.ck.entity.Order;
import com.platform.ck.entity.Result;
import com.platform.ck.service.OrderService;
import com.platform.ck.util.ResultUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import java.util.List;

/**
 * @author AllDataDC
 * @date 2022/05/05
 */
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @RequestMapping(value = "/addOrder", method = RequestMethod.POST)
    public Result<Object> addOrder(@RequestBody Order order) {

        int result = orderService.insertOrder(order);
        return new ResultUtil<Object>().setData(result);
    }

    @RequestMapping(value = "/orderList", method = RequestMethod.POST)
    public Result<List<Order>> getOrderList(@RequestBody Order order) {

        List<Order> list = orderService.getOrderById(order.getId());
        return new ResultUtil<List<Order>>().setData(list);
    }

    @RequestMapping(value = "/orderEdit", method = RequestMethod.POST)
    public Result<Object> orderEdit(@RequestBody Order order) {
        int result = orderService.updateOrder(order);
        return new ResultUtil<Object>().setData(result);
    }

    @RequestMapping(value = "/orderDel", method = RequestMethod.POST)
    public Result<Object> deleteOrderItem(@RequestBody Order order) {

        int result = orderService.deleteOrder(order.getId());
        return new ResultUtil<Object>().setData(result);
    }

}
