package com.platform.mall.service.mobile;

import com.platform.mall.entity.mobile.LitemallOrderGoods;

import java.util.List;

public interface LitemallOrderGoodsService {
    int add(LitemallOrderGoods orderGoods);

    List<LitemallOrderGoods> queryByOid(Integer orderId);

    List<LitemallOrderGoods> findByOidAndGid(Integer orderId, Integer goodsId);

    LitemallOrderGoods findById(Integer id);

    void updateById(LitemallOrderGoods orderGoods);

    Short getComments(Integer orderId);

    boolean checkExist(Integer goodsId);
}
