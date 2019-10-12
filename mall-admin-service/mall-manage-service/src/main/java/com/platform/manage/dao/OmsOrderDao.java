package com.platform.manage.dao;

import com.platform.manage.dto.OmsOrderDeliveryParam;
import com.platform.manage.dto.OmsOrderDetail;
import com.platform.manage.dto.OmsOrderQueryParam;
import com.platform.manage.model.OmsOrder;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单自定义查询Dao
 * Created by wulinhao on 2019/9/12.
 */
public interface OmsOrderDao {
    /**
     * 条件查询订单
     */
    List<OmsOrder> getList(@Param("queryParam") OmsOrderQueryParam queryParam);

    /**
     * 批量发货
     */
    int delivery(@Param("list") List<OmsOrderDeliveryParam> deliveryParamList);

    /**
     * 获取订单详情
     */
    OmsOrderDetail getDetail(@Param("id") Long id);
}
