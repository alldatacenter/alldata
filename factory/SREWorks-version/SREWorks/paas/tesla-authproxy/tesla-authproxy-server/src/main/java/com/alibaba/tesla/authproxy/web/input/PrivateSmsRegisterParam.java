package com.alibaba.tesla.authproxy.web.input;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import java.io.Serializable;

@Data
public class PrivateSmsRegisterParam implements Serializable {
    private String endpoint;
    private String token;
}
