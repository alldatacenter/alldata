package com.platform.ck.mapper;

import com.platform.ck.entity.Order;


import java.util.List;

/**
 * @author AllDataDC
 * @date 2022/05/05
 */
public interface OrderMapper {
    Integer selectTotalAmountSkuId(String skuId);
    int insertOrder(Order order);

    int deleteOrder(Integer id);

    int updateOrder(Order id);


    List<Order> selectOrder(Integer id);


}
