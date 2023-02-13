package com.platform.ck.entity;

import lombok.Data;
import java.util.Date;

/**
 * @author AllDataDC
 * @date 2023/01/05
 */
@Data
public class Order {
    private Integer id;
    private String skuId;
    private Integer totalAmount;
    private Date createTime;


}
