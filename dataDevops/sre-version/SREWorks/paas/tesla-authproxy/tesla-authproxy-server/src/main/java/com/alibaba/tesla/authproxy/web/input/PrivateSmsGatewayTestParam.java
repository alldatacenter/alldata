package com.alibaba.tesla.authproxy.web.input;

import lombok.Data;

import java.io.Serializable;

@Data
public class PrivateSmsGatewayTestParam implements Serializable {
    private String phone;
    private String aliyunId;
    private String code;
    private String content;
    private String signature;
    private Long timestamp;
    private Integer nonce;
}
