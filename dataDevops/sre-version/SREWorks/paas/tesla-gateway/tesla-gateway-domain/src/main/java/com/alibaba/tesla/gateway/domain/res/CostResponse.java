package com.alibaba.tesla.gateway.domain.res;

import lombok.Data;

import java.io.Serializable;

/**
 * cost请求耗时查询返回结果数据
 * @author tandong.td
 */
@Data
public class CostResponse implements Serializable {
    private static final long serialVersionUID = -2743145831945428635L;

    private double minCost;

    private double maxCost;

    private double avgCost;

    private long time;

}
