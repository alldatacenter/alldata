package com.platform.portal.service;

import com.platform.model.OmsCartItem;
import com.platform.portal.domain.CartPromotionItem;

import java.util.List;

/**
 * Created by wulinhao on 2019/8/27.
 * 促销管理Service
 */
public interface OmsPromotionService {
    /**
     * 计算购物车中的促销活动信息
     *
     * @param cartItemList 购物车
     */
    List<CartPromotionItem> calcCartPromotion(List<OmsCartItem> cartItemList);
}
