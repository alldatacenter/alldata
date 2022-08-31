package com.platform.mall.service.mobile;

import com.platform.mall.entity.mobile.LitemallIssue;

import java.util.List;

public interface LitemallIssueService {
    void deleteById(Integer id);

    void add(LitemallIssue issue);

    List<LitemallIssue> querySelective(String question, Integer page, Integer limit, String sort, String order);

    int updateById(LitemallIssue issue);

    LitemallIssue findById(Integer id);
}
