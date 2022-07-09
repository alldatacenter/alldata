package com.platform.mall.service.mobile;

import com.platform.mall.entity.mobile.LitemallCollect;

import java.util.List;

public interface LitemallCollectService {

    int count(int uid, Integer gid);

    List<LitemallCollect> queryByType(Integer userId, Byte type, Integer page,
                                      Integer limit, String sort, String order);

    int countByType(Integer userId, Byte type);

    LitemallCollect queryByTypeAndValue(Integer userId, Byte type, Integer valueId);

    void deleteById(Integer id);

    int add(LitemallCollect collect);

    List<LitemallCollect> querySelective(String userId, String valueId, Integer page,
                                         Integer size, String sort, String order);
}
