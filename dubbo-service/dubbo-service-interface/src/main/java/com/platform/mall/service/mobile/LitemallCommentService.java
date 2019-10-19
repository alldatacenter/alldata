package com.platform.mall.service.mobile;

import com.platform.mall.entity.mobile.LitemallComment;

import java.util.List;

public interface LitemallCommentService {
    List<LitemallComment> queryGoodsByGid(Integer id, int offset, int limit);

    List<LitemallComment> query(Byte type, Integer valueId, Integer showType, Integer offset, Integer limit);

    int count(Byte type, Integer valueId, Integer showType);

    int save(LitemallComment comment);

    List<LitemallComment> querySelective(String userId, String valueId, Integer page,
                                         Integer size, String sort, String order);

    void deleteById(Integer id);

    String queryReply(Integer id);

    LitemallComment findById(Integer id);
}
