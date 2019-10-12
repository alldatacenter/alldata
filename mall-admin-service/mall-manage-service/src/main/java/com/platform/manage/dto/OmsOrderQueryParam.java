package com.platform.manage.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 订单查询参数
 * Created by wulinhao on 2019/9/11.
 */
@Getter
@Setter
public class OmsOrderQueryParam {
    @ApiModelProperty(value = "订单编号")
    private String orderSn;
    @ApiModelProperty(value = "收货人姓名/号码")
    private String receiverKeyword;
    @ApiModelProperty(value = "订单状态：0->待付款；1->待发货；2->已发货；3->已完成；4->已关闭；5->无效订单")
    private Integer status;
    @ApiModelProperty(value = "订单类型：0->正常订单；1->秒杀订单")
    private Integer orderType;
    @ApiModelProperty(value = "订单来源：0->PC订单；1->app订单")
    private Integer sourceType;
    @ApiModelProperty(value = "确认收货状态：0->未确认；1->已确认")
    private Integer confirmStatus;
    @ApiModelProperty(value = "订单提交时间")
    private String createTime;
}
