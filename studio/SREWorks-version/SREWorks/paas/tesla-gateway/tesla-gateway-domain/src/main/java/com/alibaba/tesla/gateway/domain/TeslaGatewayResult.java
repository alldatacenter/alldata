package com.alibaba.tesla.gateway.domain;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class TeslaGatewayResult implements Serializable {
    private static final long serialVersionUID = 5645835807392347405L;

    public static final String GATEWAY_FLAG = "Tesla-Gateway";

    private int status;

    private String message;

    private String path;

    private String requestId;

    private Date timestamp;

    private String from;
}
