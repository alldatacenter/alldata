package com.alibaba.tesla.gateway.domain.res;

import lombok.Data;

import java.io.Serializable;

/**
 * Qps查询返回结果数据
 * @author tandong.td
 */
@Data
public class QpsResponse implements Serializable {
    private static final long serialVersionUID = 7005200964087378750L;

    private long time;

    private int count;
}
