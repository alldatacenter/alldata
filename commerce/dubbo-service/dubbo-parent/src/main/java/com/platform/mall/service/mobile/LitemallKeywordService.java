package com.platform.mall.service.mobile;

import com.platform.mall.entity.mobile.LitemallKeyword;

import java.util.List;

public interface LitemallKeywordService {
    LitemallKeyword queryDefault();

    List<LitemallKeyword> queryHots();

    List<LitemallKeyword> queryByKeyword(String keyword, Integer page, Integer limit);

    List<LitemallKeyword> querySelective(String keyword, String url, Integer page, Integer limit,
                                         String sort, String order);

    void add(LitemallKeyword keywords);

    LitemallKeyword findById(Integer id);

    int updateById(LitemallKeyword keywords);

    void deleteById(Integer id);
}
