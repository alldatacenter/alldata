package com.platform.manage.service;

import com.platform.mbg.model.OmsCompanyAddress;

import java.util.List;

/**
 * 收货地址管Service
 * Created by wulinhao on 2019/9/18.
 */
public interface OmsCompanyAddressService {
    /**
     * 获取全部收货地址
     */
    List<OmsCompanyAddress> list();
}
