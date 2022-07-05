package com.platform.mall.service.mobile;

import com.platform.mall.entity.mobile.LitemallGoodsAttribute;

import java.util.List;

public interface LitemallGoodsAttributeService {
    List<LitemallGoodsAttribute> queryByGid(Integer goodsId);

    void add(LitemallGoodsAttribute goodsAttribute);

    LitemallGoodsAttribute findById(Integer id);

    void deleteByGid(Integer gid);
}
