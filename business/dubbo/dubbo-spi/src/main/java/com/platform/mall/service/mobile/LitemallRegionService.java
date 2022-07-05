package com.platform.mall.service.mobile;

import com.platform.mall.entity.mobile.LitemallRegion;

import java.util.List;

public interface LitemallRegionService {

    List<LitemallRegion> getAll();

    List<LitemallRegion> queryByPid(Integer parentId);

    LitemallRegion findById(Integer id);

    List<LitemallRegion> querySelective(String name, Integer code, Integer page,
                                        Integer size, String sort, String order);
}
