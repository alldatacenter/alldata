package com.platform.mall.service.mobile;

import com.platform.mall.entity.mobile.LitemallOrder;

import java.util.List;
import java.util.Map;

public interface LitemallOrderService {

    int add(LitemallOrder order);

    int count(Integer userId);

    LitemallOrder findById(Integer orderId);

    int countByOrderSn(Integer userId, String orderSn);

    // TODO 使用分布式ID生成方案产生一个唯一的订单
    String generateOrderSn(Integer userId);

    List<LitemallOrder> queryByOrderStatus(Integer userId, List<Short> orderStatus,
                                           Integer page, Integer limit, String sort, String order);

    List<LitemallOrder> querySelective(Integer userId, String orderSn, List<Short> orderStatusArray,
                                       Integer page, Integer limit, String sort, String order);

    int updateWithOptimisticLocker(LitemallOrder order);

    void deleteById(Integer id);

    int count();

    List<LitemallOrder> queryUnpaid(int minutes);

    List<LitemallOrder> queryUnconfirm(int days);

    LitemallOrder findBySn(String orderSn);

    Map<Object, Object> orderInfo(Integer userId);

    List<LitemallOrder> queryComment(int days);
}
