package com.platform.mall.service.mobile;

import com.platform.mall.entity.mobile.LitemallStorage;

import java.util.List;

public interface LitemallStorageService {

    void deleteByKey(String key);

    void add(LitemallStorage storageInfo);

    LitemallStorage findByKey(String key);

    int update(LitemallStorage storageInfo);

    LitemallStorage findById(Integer id);

    List<LitemallStorage> querySelective(String key, String name, Integer page,
                                         Integer limit, String sort, String order);
}
