package com.platform.mall.service.admin;

import com.platform.mall.entity.admin.OmsCompanyAddress;

import java.util.List;

/**
 * 收货地址管Service
 * @author AllDataDC
 */
public interface OmsCompanyAddressService {
    /**
     * 获取全部收货地址
     */
    List<OmsCompanyAddress> list();
}
