package com.platform.base.repository;

import com.platform.base.domain.MemberProductCollection;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * 商品收藏Repository
 * Created by wulinhao on 2019/8/2.
 */
public interface MemberProductCollectionRepository extends MongoRepository<MemberProductCollection, String> {
    MemberProductCollection findByMemberIdAndProductId(Long memberId, Long productId);

    int deleteByMemberIdAndProductId(Long memberId, Long productId);

    List<MemberProductCollection> findByMemberId(Long memberId);
}
