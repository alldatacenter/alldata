package com.alibaba.tesla.authproxy.oauth2client.dto;

import lombok.Builder;
import lombok.Data;

/**
 * @author cdx
 * @date 2020/1/2 20:43
 */
@Data
@Builder
public class TeslaJwtUserInfoDTO {
    private String userId;
    private String loginName;
    private String bucId;
    private String aliyunPk;
    private String nickName;
    private String empId;
    private String appId;
}
