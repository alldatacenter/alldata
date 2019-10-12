package com.platform.mobile.service;

import com.platform.manage.model.OmsCartItem;
import com.platform.mobile.domain.CartPromotionItem;

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
