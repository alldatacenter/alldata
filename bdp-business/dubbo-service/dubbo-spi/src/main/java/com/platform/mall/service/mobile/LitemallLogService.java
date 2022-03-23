package com.platform.mall.service.mobile;

import com.platform.mall.entity.mobile.LitemallLog;

import java.util.List;

public interface LitemallLogService {
    void add(LitemallLog log);

    List<LitemallLog> querySelective(String name, Integer page, Integer size, String sort, String order);
}
