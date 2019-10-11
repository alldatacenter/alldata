package com.platform.service;

import com.platform.model.OmsOrderSetting;

/**
 * 订单设置Service
 * Created by wulinhao on 2019/9/16.
 */
public interface OmsOrderSettingService {
    /**
     * 获取指定订单设置
     */
    OmsOrderSetting getItem(Long id);

    /**
     * 修改指定订单设置
     */
    int update(Long id, OmsOrderSetting orderSetting);
}
