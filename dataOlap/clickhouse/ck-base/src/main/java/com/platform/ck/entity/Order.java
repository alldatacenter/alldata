package com.platform.ck.entity;

import lombok.Data;
import java.util.Date;

/**
 * @author wlhbdp
 * @date 2022/05/05
 */
@Data
public class Order {
    private Integer id;
    private String skuId;
    private Integer totalAmount;
    private Date createTime;


}
