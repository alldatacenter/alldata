package com.platform.mall.service.mobile;

import com.platform.mall.entity.mobile.LitemallGoodsProduct;

import java.util.List;

public interface LitemallGoodsProductService {
    List<LitemallGoodsProduct> queryByGid(Integer gid);

    LitemallGoodsProduct findById(Integer id);

    void deleteById(Integer id);

    void add(LitemallGoodsProduct goodsProduct);

    int count();

    void deleteByGid(Integer gid);

    int addStock(Integer id, Short num);

    int reduceStock(Integer id, Short num);
}
