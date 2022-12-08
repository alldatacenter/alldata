package com.platform.mall.service.mobile;

import com.platform.mall.entity.mobile.LitemallAd;
import java.util.List;

public interface LitemallAdService {
    List<LitemallAd> queryIndex();

    List<LitemallAd> querySelective(String name, String content, Integer page, Integer limit, String sort, String order);

    int updateById(LitemallAd ad);

    void deleteById(Integer id);

    void add(LitemallAd ad);

    LitemallAd findById(Integer id);
}
