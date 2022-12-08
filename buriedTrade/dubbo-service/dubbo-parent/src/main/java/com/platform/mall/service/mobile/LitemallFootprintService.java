package com.platform.mall.service.mobile;

import com.platform.mall.entity.mobile.LitemallFootprint;

import java.util.List;

public interface LitemallFootprintService {
    List<LitemallFootprint> queryByAddTime(Integer userId, Integer page, Integer size);

    LitemallFootprint findById(Integer id);

    void deleteById(Integer id);

    void add(LitemallFootprint footprint);

    List<LitemallFootprint> querySelective(String userId, String goodsId, Integer page, Integer size,
                                           String sort, String order);
}
