package com.platform.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 订单修改收货人信息参数
 * Created by wulinhao on 2019/9/29.
 */
@Getter
@Setter
public class OmsReceiverInfoParam {
    private Long orderId;
    private String receiverName;
    private String receiverPhone;
    private String receiverPostCode;
    private String receiverDetailAddress;
    private String receiverProvince;
    private String receiverCity;
    private String receiverRegion;
    private Integer status;
}
