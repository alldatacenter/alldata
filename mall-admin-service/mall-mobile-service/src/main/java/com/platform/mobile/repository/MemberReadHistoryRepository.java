package com.platform.mobile.repository;

import com.platform.mobile.domain.MemberReadHistory;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * 会员商品浏览历史Repository
 * Created by wulinhao on 2019/8/3.
 */
public interface MemberReadHistoryRepository extends MongoRepository<MemberReadHistory, String> {
    List<MemberReadHistory> findByMemberIdOrderByCreateTimeDesc(Long memberId);
}
