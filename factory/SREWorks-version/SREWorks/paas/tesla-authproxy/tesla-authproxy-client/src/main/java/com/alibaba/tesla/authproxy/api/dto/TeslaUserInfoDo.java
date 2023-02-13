package com.alibaba.tesla.authproxy.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeslaUserInfoDo {
    private String loginName;
    private String nickName;
    private String phone;
    private String accessKeyId = "";
    private String accessKeySecret = "";
    private String secretKey = "";
}
