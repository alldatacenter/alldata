package com.platform.mall.service.mobile;

import com.platform.mall.entity.mobile.LitemallTopic;

import java.util.List;

public interface LitemallTopicService {
    List<LitemallTopic> queryList(int offset, int limit);

    List<LitemallTopic> queryList(int offset, int limit, String sort, String order);

    int queryTotal();

    LitemallTopic findById(Integer id);

    List<LitemallTopic> queryRelatedList(Integer id, int offset, int limit);

    List<LitemallTopic> querySelective(String title, String subtitle, Integer page,
                                       Integer limit, String sort, String order);

    int updateById(LitemallTopic topic);

    void deleteById(Integer id);

    void add(LitemallTopic topic);


}
