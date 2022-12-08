package com.platform.mall.service.mobile;

import com.platform.mall.entity.mobile.LitemallSearchHistory;

import java.util.List;

public interface LitemallSearchHistoryService {

    void save(LitemallSearchHistory searchHistory);

    List<LitemallSearchHistory> queryByUid(int uid);

    void deleteByUid(int uid);

    List<LitemallSearchHistory> querySelective(String userId, String keyword, Integer page,
                                               Integer size, String sort, String order);
}
