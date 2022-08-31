package com.platform.mall.service;

import com.platform.mall.entity.TbAddress;

import java.util.List;

/**
 * @author wulinhao
 */
public interface AddressService {

    /**
     * 获取地址
     * @param userId
     * @return
     */
    List<TbAddress> getAddressList(Long userId);

    /**
     * 获取单个
     * @param addressId
     * @return
     */
    TbAddress getAddress(Long addressId);

    /**
     * 添加
     * @param tbAddress
     * @return
     */
    int addAddress(TbAddress tbAddress);

    /**
     * 更新
     * @param tbAddress
     * @return
     */
    int updateAddress(TbAddress tbAddress);

    /**
     * 删除
     * @param tbAddress
     * @return
     */
    int delAddress(TbAddress tbAddress);
}
