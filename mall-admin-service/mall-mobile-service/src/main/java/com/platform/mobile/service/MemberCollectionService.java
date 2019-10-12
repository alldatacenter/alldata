package com.platform.mobile.service;

import com.platform.mobile.domain.MemberProductCollection;

import java.util.List;

/**
 * 会员收藏Service
 * Created by wulinhao on 2019/8/2.
 */
public interface MemberCollectionService {
    int addProduct(MemberProductCollection productCollection);

    int deleteProduct(Long memberId, Long productId);

    List<MemberProductCollection> listProduct(Long memberId);
}
