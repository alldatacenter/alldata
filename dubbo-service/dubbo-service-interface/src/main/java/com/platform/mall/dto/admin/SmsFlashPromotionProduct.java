package com.platform.mall.dto.admin;

import com.platform.mall.entity.admin.PmsProduct;
import com.platform.mall.entity.admin.SmsFlashPromotionProductRelation;

/**
 * 限时购及商品信息封装
 * Created by wulinhao on 2019/9/16.
 */
public class SmsFlashPromotionProduct extends SmsFlashPromotionProductRelation {

    private PmsProduct product;

    public PmsProduct getProduct() {
        return product;
    }

    public void setProduct(PmsProduct product) {
        this.product = product;
    }
}
