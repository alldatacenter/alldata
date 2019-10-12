package com.platform.manage.dto;

import com.platform.mbg.model.PmsProduct;
import com.platform.mbg.model.SmsFlashPromotionProductRelation;
import lombok.Getter;
import lombok.Setter;

/**
 * 限时购及商品信息封装
 * Created by wulinhao on 2019/9/16.
 */
public class SmsFlashPromotionProduct extends SmsFlashPromotionProductRelation {
    @Getter
    @Setter
    private PmsProduct product;
}
